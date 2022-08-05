# Project details
1. This code implementation is written in Java programming language.
2. This is the project for the MCDP certification and is based on the tasks (assignment) given for it.
3. Below are the installation instructions, tested use cases, scripts.

# Installation instructions
1. Clone the repository by running the `git clone` command.
2. Java jdk to be installed and setup on your machine.
3. Maven (mvn) should be installed and setup on your machine

# Running application
1. `mvn compile`
2. `mvn package`
3. `java -jar <JAR_NAME> <MONGODB_CONNECTION_STRING>`
4. By default application runs on MongoDB `localhost:27017`
5. Application runs on port `localhost:5001`. Change the port in applicatio as per requirement.

# Tested

1. This application is successfully tested with multiple scenarios
   - 2102 cities and 200 planes
   - 5000 cities and 400 planes
   - 141 cities and 20 planes

# Load initial data
1. Initially load all the cities given with worldcities.csv.

```
mongoimport --uri mongodb://localhost:27017 --headerline -d logistics -c worldcities --type=csv worldcities.csv
mongo --host localhost --port 27017
show dbs
use logistics
```

# Initial data scripts
```
minsize = {$match:{population:{$gt:500000}}}
sortbysize = { $sort : { population: -1 }}
onepercountry = { $group : { _id: "$country", city : { $first : "$$ROOT" }}}
format = { $project : { _id: "$city.city_ascii", name: "$city.city_ascii", location:["$city.lng","$city.lat"] , country: "$city.country" }}
newcollection = { $out : "cities" }
db.worldcities.aggregate([minsize,sortbysize,onepercountry,format,newcollection])

firstN = { $sample: { size: 20} }
addidone = { $group: { _id: null, planes : { $push : { currentLocation :"$location", location :"$_id" }}}}
unwind = { $unwind : {path: "$planes", includeArrayIndex: "id" }}
format = {$project : { _id : {$concat : ["CARGO",{$toString:"$id"}]}, callsign : {$concat : ["CARGO",{$toString:"$id"}]},currentLocation: "$planes.currentLocation", landed:"$planes.location", heading:{$literal:0.0}, route: []}}
asplanes = { $out: "planes"}
db.cities.aggregate([firstN,addidone,unwind,format,asplanes])

db.cities.createIndex( { "location" : "2dsphere" } )
db.cargos.createIndex( { "location" : 1, "status":1 } )
```

# Scripts and queries for task 3
To run application on top 15 cities of countries with population greater than 1000.

## Script for city collection
This script does the removal of '/' charater from the cities names. This is because '/' character creates problem with API URI.

```
matchByMinPopulation = { $match: {population:{$gt:1000}}}
sortByPopulation = { $sort : { population: -1 }}
groupByCountry = { $group: { _id: "$country", city : { $push : '$$ROOT' }}}
projectForSlicing = { $project: {_id:1, "city": { "$slice": [ "$city", 15 ] }}}
unwindCity = { $unwind: "$city"}
addFieldsCityCharArray = { $addFields: { cityCharArray: {"$map": {input: { "$range": [ 0, { "$strLenCP": "$city.city_ascii" } ] },in: { "$substrCP": ["$city.city_ascii", "$$this", 1 ] }}}}}
addFieldsFilteredCityCharArray = { $addFields: { filteredCityCharArray: { "$filter": {input: "$cityCharArray", cond: { "$regexMatch": { input: "$$this", regex: "^[^/]+$" } }}}}}
addFieldsFilteredCityString = { $addFields: { filteredCityString: {"$reduce": { input: "$filteredCityCharArray", initialValue: "", in: { "$concat": [ "$$value", "$$this" ] }}}}}
projectCity = { $project : { _id: {$concat : ["$filteredCityString", "-", "$city.country"]}, name: {$concat : ["$filteredCityString", "-", "$city.country"]}, location:["$city.lng","$city.lat"] , country: "$city.country" }}
outCities = { $out : "cities" }
db.worldcities.aggregate([matchByMinPopulation,sortByPopulation,groupByCountry,projectForSlicing,unwindCity,addFieldsCityCharArray,addFieldsFilteredCityCharArray,addFieldsFilteredCityString,projectCity,outCities])
```

## Script for plane collection
This is a modified script, to create 200 planes.

```
firstN = { $sample: { size: 200} }
addidone = { $group: { _id: null, planes : { $push : { currentLocation :"$location", location :"$_id" }}}}
unwind = { $unwind : {path: "$planes", includeArrayIndex: "id" }}
format = {$project : { _id : {$concat : ["CARGO",{$toString:"$id"}]}, callsign : {$concat : ["CARGO",{$toString:"$id"}]},currentLocation: "$planes.currentLocation", landed:"$planes.location", heading:{$literal:0}, route: []}}
asplanes = { $out: "planes"}
db.cities.aggregate([firstN,addidone,unwind,format,asplanes])
```
## indexes for collections
```
db.cities.createIndex( { "location" : "2dsphere" } )
db.cargos.createIndex( { "location" : 1, "status":1 } )
```
