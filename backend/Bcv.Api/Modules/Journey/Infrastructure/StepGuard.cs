namespace Bcv.Api.Modules.Journey.Infrastructure;

/// <summary>
/// F87 step plausibility: a sync may add at most a brisk walking rate since the previous sync
/// (plus a small allowance for batched sensor data), and a journey may never exceed 40,000 steps
/// per started day.
/// </summary>
public static class StepGuard
{
    public const int MaxStepsPerMinute = 250;
    public const int BurstAllowance = 1000;
    public const int MaxStepsPerDay = 40_000;

    /// <summary>Set from configuration <c>StepGuard:Enabled</c> (default on). Turn off only for demos
    /// that use the debug step simulator, which sends large jumps on purpose.</summary>
    public static bool Enabled { get; set; } = true;

    public static int MaxNewSteps(double minutesSinceLastSync, double minutesSinceStart, int previousSteps)
    {
        if (!Enabled) return int.MaxValue;
        var byRate = BurstAllowance + Math.Max(0, minutesSinceLastSync) * MaxStepsPerMinute;
        var startedDays = Math.Max(1, Math.Ceiling(Math.Max(0, minutesSinceStart) / (24 * 60)));
        var byDay = startedDays * MaxStepsPerDay - previousSteps;
        return (int)Math.Max(0, Math.Min(Math.Min(byRate, byDay), int.MaxValue));
    }
}
