# Local API integration checks

PASS: Backend local health and target database verified before fixture writes

PASS: Search requires authentication

PASS: Register isolated test account through real API

PASS: All-card album includes every active card independently of themed collection publication

PASS: Unowned active cards expose artwork for preview

PASS: Album preview cannot award card ownership

PASS: Filter options use twelve standard place types

PASS: Profile offers twelve distinct standard interests

PASS: Legacy six-group selection remains stored while UI requests reselection

PASS: Twelve-type selections persist and reload

PASS: New standard interests integrate with recommendation model

PASS: New coast/island interest filter agrees with user preference matching

PASS: Multiple checked destination categories combine without duplicate places

PASS: Preference sort places matching interests before other filtered destinations

PASS: Unknown checked category is rejected

PASS: Four interests rejected without overwriting saved choices

PASS: Search pagination and total

PASS: Search pages contain no duplicate destinations

PASS: User can search destinations outside selected heritage interests

PASS: Combined distance and time filters

PASS: Invalid reversed range is rejected

PASS: Empty page retains accurate total

PASS: Preview effort uses current activity and unchanged step formula

PASS: Activity changes duration without changing required journey steps

PASS: Preview does not record discovery or passport stamps

PASS: Empty regions cannot earn a badge

PASS: Existing create-journey flow remains usable

PASS: Recommendation API returns up to three distinct valid candidates

PASS: Weekly quest API uses real ISO cycle and configured quests

PASS: Ordinary user cannot access admin place catalog

PASS: New user has no historical ranking points

PASS: Leaderboard excludes private account and location fields

PASS: Invalid ISO week is rejected

PASS: Repeated cumulative step fixture cannot duplicate progress

PASS: Concurrent completion requests both resolve successfully

PASS: Journey completion confirms card award

PASS: Completion response confirms 150 unified discovery points

PASS: Completion receipt requires authentication

PASS: Read-only completion receipt confirms the owned completed journey

PASS: Receipt returns actual awarded points and recorded journey steps

PASS: Receipt shows only the card actually received at completion

PASS: Other users cannot read completion receipts

PASS: Repeated completion grants one collectible card

PASS: Completion adds exactly one passport stamp

PASS: First walking completion records approved first-place and journey points

PASS: Regional ranking counts destination events, excluding regionless quest points

PASS: Weekly quest reward pays once across repeated reads

PASS: Repeated sync, leaderboard and quest reads cannot add points

PASS: Opening and retrying completion receipts never awards points

PASS: Flight unlock and its discovery quests do not earn walking points

PASS: Walking and flight discoveries cannot be selected for another journey

PASS: Walking and flight discoveries cannot be selected for another journey

PASS: Rejected revisits leave destination ranking points unchanged

PASS: Destination picker excludes discovered places and current zero-step location before pagination

PASS: Both walking and flight discoveries disappear from selectable destinations

PASS: Replace-active cannot bypass the discovered-place restriction

PASS: Rejected discovered destination preserves the existing active journey

PASS: Historical completion remains readable while another journey is active

PASS: An unfinished journey cannot produce a success receipt

PASS: Legacy in-progress revisit cannot complete through sync-steps

PASS: Rejected legacy completion rolls back journey progress

PASS: Rejected legacy completion cannot award journey, discovery or quest points

Test fixture account created through registration on v2, using example.invalid. No real user copied. Synthetic step fixtures submitted: True. This does not verify physical sensor/Health Connect. Test credentials/tokens not exported.
