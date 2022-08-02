# Scripts and queries for collection

Application is running on port 5001.

# Load sample data - run following commands in sequence. Check for the description given for each command.

mongoimport --uri mongodb://localhost:27017 --headerline -d logistics -c worldcities --type=csv worldcities.csv

mongo --host localhost --port 27017
show dbs
use logistics

# Script for city collection

matchByMinPopulation = { $match: {population:{$gt:1000}}}
sortByPopulation = { $sort : { population: -1 }}
groupByCountry = { $group: { _id: "$country", city : { $push : "$$ROOT" }}}
projectForSlicing = { $project: {_id:1, "city": { "$slice": [ "$city", 15 ] }}}
unwindCity = { $unwind: "$city"}
addFieldsCityCharArray = { $addFields: { cityCharArray: {"$map": {input: { "$range": [ 0, { "$strLenCP": "$city.city_ascii" } ] },in: { "$substrCP": ["$city.city_ascii", "$$this", 1 ] }}}}}
addFieldsFilteredCityCharArray = { $addFields: { filteredCityCharArray: { "$filter": {input: "$cityCharArray", cond: { "$regexMatch": { input: "$$this", regex: "^[^/]+$" } }}}}}
addFieldsFilteredCityString = { $addFields: { filteredCityString: {"$reduce": { input: "$filteredCityCharArray", initialValue: "", in: { "$concat": [ "$$value", "$$this" ] }}}}}
projectCity = { $project : { _id: {$concat : ["$filteredCityString", "-", "$city.country"]}, name: {$concat : ["$filteredCityString", "-", "$city.country"]}, location:["$city.lng","$city.lat"] , country: "$city.country" }}
outCities = { $out : "cities" }
db.worldcities.aggregate([matchByMinPopulation,sortByPopulation,groupByCountry,projectForSlicing,unwindCity,addFieldsCityCharArray,addFieldsFilteredCityCharArray,addFieldsFilteredCityString,projectCity,outCities])

# Script for plane collection

firstN = { $sample: { size: 200} }
addidone = { $group: { _id: null, planes : { $push : { currentLocation :"$location", location :"$_id" }}}}
unwind = { $unwind : {path: "$planes", includeArrayIndex: "id" }}
format = {$project : { _id : {$concat : ["CARGO",{$toString:"$id"}]}, callsign : {$concat : ["CARGO",{$toString:"$id"}]},currentLocation: "$planes.currentLocation", landed:"$planes.location", heading:{$literal:0}, route: []}}
asplanes = { $out: "planes"}
db.cities.aggregate([firstN,addidone,unwind,format,asplanes])

# indexes for collections

db.cities.createIndex( { "location" : "2dsphere" } )
db.cargos.createIndex( { "location" : 1, "status":1 } )
