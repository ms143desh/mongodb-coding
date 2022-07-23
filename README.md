
# Load sample data - run following commands in sequence. Check for the description given for each command.

mongoimport --uri mongodb://localhost:27017 --headerline -d logistics -c worldcities --type=csv worldcities.csv

mongo --host localhost --port 27017
show dbs
use logistics


#This script is a part of task 3
#This script is used to create a collection "cities". cities collection include the 15 largest cities in every country (of fewer if there are less than 15).
#This script should not be used to run simulation. Because, this contains some cities which have special characters like ("/") in their name, because of which API URI fails. 
#Also, this script updates the city name with pattern "city-country". This is because, there are multiple records which have the same city name but different country like "George Town"

# Aggregation Script City

matchByMinPopulation = { $match: {population:{$gt:1000}}}
sortByPopulation = { $sort : { population: -1 }}
groupByCountry = { $group: { _id: "$country", city : { $push : "$$ROOT" }}}
projectForSlicing = { $project: {_id:1, "city": { "$slice": [ "$city", 15 ] }}}
unwindCity = { $unwind: "$city"}
projectCity = { $project : { _id: {$concat : ["$city.city_ascii", "-", "$city.country"]}, position:["$city.lng","$city.lat"] , country: "$city.country" }}
outCities = { $out : "cities" }
db.worldcities.aggregate([matchByMinPopulation,sortByPopulation,groupByCountry,projectForSlicing,unwindCity,projectCity,outCities])


#This script is a part of task 3
#This script is used to create a collection "cities". cities collection includes the 15 largest cities in every country (of fewer if there are less than 15).
#This script is modified version of the above script. This script removes the special characters like ("/") from the city names.
#Also, this script updates the city name with pattern "city-country". This is because, there are multiple records which have the same city name but different country like "George Town"
#This script can be used to run simulation. Because, this contains some cities which have special characters like ("/") in their name, because of which API URI fails.

# Aggregation Script City

matchByMinPopulation = { $match: {population:{$gt:1000}}}
sortByPopulation = { $sort : { population: -1 }}
groupByCountry = { $group: { _id: "$country", city : { $push : "$$ROOT" }}}
projectForSlicing = { $project: {_id:1, "city": { "$slice": [ "$city", 15 ] }}}
unwindCity = { $unwind: "$city"}
addFieldsCityCharArray = { $addFields: { cityCharArray: {'$map': {input: { '$range': [ 0, { '$strLenCP': '$city.city_ascii' } ] },in: { '$substrCP': [ '$city.city_ascii', '$$this', 1 ] }}}}}
addFieldsFilteredCityCharArray = { $addFields: { filteredCityCharArray: { '$filter': {input: '$cityCharArray', cond: { '$regexMatch': { input: '$$this', regex: '^[^/]+$' } }}}}}
addFieldsFilteredCityString = { $addFields: { filteredCityString: {'$reduce': { input: '$filteredCityCharArray', initialValue: '', in: { '$concat': [ '$$value', '$$this' ] }}}}}
projectCity = { $project : { _id: {$concat : ["$filteredCityString", "-", "$city.country"]}, position:["$city.lng","$city.lat"] , country: "$city.country" }}
outCities = { $out : "cities" }
db.worldcities.aggregate([matchByMinPopulation,sortByPopulation,groupByCountry,projectForSlicing,unwindCity,addFieldsCityCharArray,addFieldsFilteredCityCharArray,addFieldsFilteredCityString,projectCity,outCities])


#This script is used to create a collection "planes"
#This script can be used to run simulation. Update the size value as per number planes requirement.

# Aggregation Script Plane

firstN = { $sample: { size: 200} }
addidone = { $group: { _id: null, planes : { $push : { currentLocation :"$position", location :"$_id" }}}}
unwind = { $unwind : {path: "$planes", includeArrayIndex: "id" }}
format = {$project : { _id : {$concat : ["CARGO",{$toString:"$id"}]},currentLocation: "$planes.currentLocation", landed:"$planes.location", heading:{$literal:0}, route: []}}
asplanes = { $out: "planes"}
db.cities.aggregate([firstN,addidone,unwind,format,asplanes])


#This script is required to create indexes on the specific collections.
#2dsphere index is mandatory to run simulation

db.cities.createIndex( { "position" : "2dsphere" } )
db.cargos.createIndex( { "location" : 1, "status":1 } )


#Task 3 - To record plane travel history, miles travel time taken.
This code has been done as Java class PlaneRecordService.java, which is called on separate thread on plane landed.
To access the plane travel history, an additional API is also created - GET "/planes/history/:id"
This also handles the length of plane history. If plane history goes more than 40 count, then it will migrate the oldest 20 records to new collection.
A new collection "planes_travel_archives" is also created, to store the oldest plane travel history.


Unit tests are run from the python file itself. Run unit_tests.py
