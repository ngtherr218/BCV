using System.Text.Json;
using System.Security.Cryptography;
using Bcv.Api.Modules.Discovery.Application;
using Bcv.Api.Modules.Discovery.Domain;

if(args.Length!=3) throw new ArgumentException("Usage: dataset.json profiles.json output.json");
var json=new JsonSerializerOptions(JsonSerializerDefaults.Web){WriteIndented=true};
var datasetBytes=File.ReadAllBytes(args[0]);
using var dataset=JsonDocument.Parse(File.ReadAllText(args[0]));
var root=dataset.RootElement;
var config=root.GetProperty("config");
var settings=config.GetProperty("settings").Deserialize<RecommendationSettings>(json)!;
if(!settings.IsValid()) throw new ArgumentException("Invalid versioned settings");
var weights=root.GetProperty("weights").Deserialize<Dictionary<string,decimal>>(json)!;
if(!RecommendationScoring.ValidWeights(weights)) throw new ArgumentException("Invalid weights");
var limit=config.GetProperty("distance_limit").GetDecimal();
var profiles=JsonSerializer.Deserialize<Profile[]>(File.ReadAllText(args[1]),json)!;
if(profiles.Length==0 || profiles.Select(p=>p.CaseId).Distinct().Count()!=profiles.Length)
    throw new ArgumentException("Provide unique, nonempty evaluation cases");
var variants=new Dictionary<string,Dictionary<string,decimal>> {
 ["P_ONLY"]=new(){{"PREFERENCE",1},{"CLUSTER",0},{"ACTIVITY",0},{"RATING",0},{"POPULARITY",0}},
 ["P_C"]=new(){{"PREFERENCE",2m/3m},{"CLUSTER",1m-2m/3m},{"ACTIVITY",0},{"RATING",0},{"POPULARITY",0}},
 ["FULL"]=weights,
 ["DOCUMENT_INITIAL"]=new(){{"PREFERENCE",.4m},{"CLUSTER",.2m},{"ACTIVITY",.2m},{"RATING",.1m},{"POPULARITY",.1m}}
};
var withoutC=weights.ToDictionary(p=>p.Key,p=>p.Key=="CLUSTER"?0:p.Value);
var sum=withoutC.Values.Sum();
if(sum<=0) throw new ArgumentException("Cannot ablate C when all remaining weights are zero");
foreach(var key in withoutC.Keys.ToArray()) withoutC[key]/=sum;
// Remove decimal division residual without changing the ablation.
var residualKey=withoutC.First(p=>p.Value>0).Key;
withoutC[residualKey]+=1-withoutC.Values.Sum();
variants["WITHOUT_C"]=withoutC;
var results=new List<object>();
foreach(var profile in profiles) {
 if(string.IsNullOrWhiteSpace(profile.CaseId)||string.IsNullOrWhiteSpace(profile.ParticipantId)
   ||!double.IsFinite(profile.Latitude)||!double.IsFinite(profile.Longitude)
   ||profile.Latitude is <-90 or >90 || profile.Longitude is <-180 or >180
   ||profile.ActivityLevel is null || !settings.ActivityThresholds.ContainsKey(profile.ActivityLevel)
   ||profile.DiscoveredPlaceIds is null || profile.InterestIds is not {Length: >0}
   ||profile.InterestIds.Distinct().Count()!=profile.InterestIds.Length
   ||profile.InterestIds.Any(id=>!settings.InterestFeatures.ContainsKey(id))) throw new ArgumentException("Invalid profile");
 var prefs=profile.InterestIds.Select(id=>new PreferenceGroup(id.ToString(),settings.InterestFeatures[id].ToHashSet())).ToArray();
 var places=root.GetProperty("places").EnumerateArray().Select(p=>new RecommendationPlace(
   p.GetProperty("placeId").GetInt64(),p.GetProperty("name").GetString()!,p.GetProperty("regionName").GetString()!,null,
   p.GetProperty("features").EnumerateObject().Where(v=>v.Value.GetDouble()==1).Select(v=>v.Name).ToHashSet(),
   p.GetProperty("cluster").GetInt32(),p.GetProperty("rating").ValueKind==JsonValueKind.Null?null:p.GetProperty("rating").GetDecimal(),
   p.GetProperty("ratingCount").ValueKind==JsonValueKind.Null?null:p.GetProperty("ratingCount").GetInt32(),
   Distance(profile.Latitude,profile.Longitude,p.GetProperty("latitude").GetDouble(),p.GetProperty("longitude").GetDouble()),
   profile.DiscoveredPlaceIds.Contains(p.GetProperty("placeId").GetInt64()))).ToArray();
 var eligible=places.Where(p=>!p.Discovered&&p.DistanceMeters>0&&p.DistanceMeters<=limit&&p.Features.Count>0
   &&p.Features.All(RecommendationScoring.FeatureCodes.Contains)).OrderBy(p=>p.PlaceId).ToArray();
 var rankings=variants.ToDictionary(v=>v.Key,v=>RecommendationScoring.Rank(places,prefs,profile.ActivityLevel,v.Value,limit,settings.ActivityThresholds)
   .Select(p=>new {p.Place.PlaceId,p.Score,p.Criteria,p.EffectiveWeights}).ToArray());
 results.Add(new {profile.CaseId,profile.ParticipantId,profile.InterestIds,profile.ActivityLevel,
   candidates=eligible.Select(p=>new{p.PlaceId,p.Name,p.RegionName,p.DistanceMeters,
     requiredSteps=Bcv.Api.Modules.StepLocation.Domain.StepConversion.RequiredSteps(p.DistanceMeters/1000m)}),rankings});
}
File.WriteAllText(args[2],JsonSerializer.Serialize(new {schemaVersion=1,
 datasetSha256=Convert.ToHexString(SHA256.HashData(datasetBytes)).ToLowerInvariant(),
 configVersion=config.GetProperty("version").GetString(),settings,
 distanceMethod="haversine-radius-6371008.8m; same candidate distances for all offline methods",variants,cases=results},json));
Console.WriteLine($"Prepared {profiles.Length} cases; no human quality score has been calculated.");

static decimal Distance(double a,double b,double c,double d) {
 var lat=(c-a)*Math.PI/180;var lon=(d-b)*Math.PI/180;
 var h=Math.Pow(Math.Sin(lat/2),2)+Math.Cos(a*Math.PI/180)*Math.Cos(c*Math.PI/180)*Math.Pow(Math.Sin(lon/2),2);
 return (decimal)(6371008.8*2*Math.Asin(Math.Sqrt(Math.Clamp(h,0,1))));
}
record Profile(string CaseId,string ParticipantId,long[] InterestIds,string ActivityLevel,
 double Latitude,double Longitude,long[] DiscoveredPlaceIds);
