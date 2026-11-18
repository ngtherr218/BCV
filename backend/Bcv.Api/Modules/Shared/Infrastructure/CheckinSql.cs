namespace Bcv.Api.Modules.Shared.Infrastructure;

/// <summary>
/// Where a real check-in is allowed, as SQL over $1 = user_id and $2 = place_id. One rule for the place
/// page (which shows the button) and the check-in endpoint (which accepts it).
/// </summary>
public static class CheckinSql
{
    /// <summary>The virtual location counts as "at" a place within this many degrees (about 50 m).</summary>
    public const string VirtualLocationToleranceDegrees = "0.0005";

    /// <summary>The virtual location only moves when a journey ends: after this many steps of a new journey
    /// the user has left that spot.</summary>
    public const int LeftVirtualLocationSteps = 1000;

    /// <summary>The place is the destination of the journey being walked.</summary>
    public const string IsJourneyDestination =
        "EXISTS (SELECT 1 FROM journey WHERE user_id=$1 AND destination_place_id=$2 AND status='IN_PROGRESS')";

    /// <summary>The user's virtual location stands at the place and they have not walked away yet.</summary>
    public static readonly string IsAtVirtualLocation = $"""
        (EXISTS (SELECT 1 FROM virtual_location v JOIN place p ON p.place_id=$2 WHERE v.user_id=$1
                 AND abs(v.latitude - p.latitude) < {VirtualLocationToleranceDegrees}
                 AND abs(v.longitude - p.longitude) < {VirtualLocationToleranceDegrees})
         AND NOT EXISTS (SELECT 1 FROM journey j WHERE j.user_id=$1 AND j.status='IN_PROGRESS'
                         AND j.recorded_steps >= {LeftVirtualLocationSteps}))
        """;

    /// <summary>FROM-clause body: this place's active card the user does not own yet (earned by finishing a journey).</summary>
    public const string PendingCard = """
        card c WHERE c.place_id=$2 AND c.status='ACTIVE'
          AND NOT EXISTS (SELECT 1 FROM user_card uc WHERE uc.user_id=$1 AND uc.card_id=c.card_id)
        """;
}
