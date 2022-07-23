package org.example.mongodb;

import static org.example.mongodb.service.Constants.ERROR_CODE;
import static org.example.mongodb.service.Constants.MONGODB_CONNECTION_URI;
import static spark.Spark.after;
import static spark.Spark.delete;
import static spark.Spark.exception;
import static spark.Spark.externalStaticFileLocation;
import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.put;

import java.util.logging.LogManager;

import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.example.mongodb.codec.CargoCodec;
import org.example.mongodb.codec.CityCodec;
import org.example.mongodb.codec.PlaneCodec;
import org.example.mongodb.codec.PlaneRecordCodec;
import org.example.mongodb.exceptions.MCDPProjectException;
import org.example.mongodb.service.APIRoutesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

public class WebServiceMain {
	static final String version = "0.0.1";
	static Logger logger;

	public static int ordinalIndexOf(String str, String substr, int n) {
		int pos = -1;
		do {
			pos = str.indexOf(substr, pos + 1);
		} while (n-- > 0 && pos != -1);
		return pos;
	}

	public static void main(String[] args) {
		port(5001);
		String staticDir = System.getProperty("user.dir").concat("/static");
		//staticDir = staticDir.substring(0,ordinalIndexOf(staticDir,"/",2)) + "/static";
		externalStaticFileLocation(staticDir);
		LogManager.getLogManager().reset();
		
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();

		logger = LoggerFactory.getLogger(WebServiceMain.class);
		logger.info(version);

		String mongodbConnectionUri = MONGODB_CONNECTION_URI;
        if(args.length > 0)
        {
        	mongodbConnectionUri = args[0];
        }
        
        ConnectionString connectionString = new ConnectionString(mongodbConnectionUri);
        CodecRegistry pojoCodecRegistry = CodecRegistries.fromCodecs(new PlaneCodec(), new CityCodec(), new CargoCodec(), new PlaneRecordCodec());
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),pojoCodecRegistry);
        MongoClientSettings clientSettings = MongoClientSettings.builder().applyConnectionString(connectionString).codecRegistry(codecRegistry).build();
        MongoClient mongoClient = MongoClients.create(clientSettings);
        
        APIRoutesService apiRoutesService = new APIRoutesService(mongoClient);
		        exception(MCDPProjectException.class, (e, req, res) -> {
		            res.status(req.attribute(ERROR_CODE));
		            res.body(e.getMessage());
		        });	
        
        // *** PLANES ***
				//Fetch planes
				// E.G. curl -X GET http://localhost:5000/planes
				get("/planes",(req,res) -> apiRoutesService.getPlanes(req,res));

				//Fetch plane by ID
				// E.G. curl -X GET http://localhost:5000/planes/CARGO10
				get("/planes/:id",(req,res) -> apiRoutesService.getPlaneById(req,res));

				// Update location, heading, and landed for a plane
				// E.G. curl -X PUT http://localhost:5000/planes/CARGO10/location/2,3/240/London
				put("/planes/:id/location/:location/:heading/:landed",(req,res) -> apiRoutesService.updatePlaneById(req,res));

				//Update location and heading for a plane
				// E.G. curl -X PUT http://localhost:5000/planes/CARGO10/location/2,3/240
				put("/planes/:id/location/:location/:heading",(req,res) -> apiRoutesService.updatePlaneById(req,res));

				//Replace a Plane's Route with a single city
				// E.G. curl -X PUT http://localhost:5000/planes/CARGO10/route/London
				put("/planes/:id/route/:city",(req,res) -> apiRoutesService.addPlaneRoute(req,res,true));

				//Add a city to a Plane's Route
				// E.G. curl -X POST http://localhost:5000/planes/CARGO10/route/London
				post("/planes/:id/route/:city",(req,res) -> apiRoutesService.addPlaneRoute(req,res,false));

				//Remove the first entry in the list of a Planes route
				// E.G. curl -X DELETE http://localhost:5000/planes/CARGO10/route/destination
				delete("/planes/:id/route/destination",(req,res) -> apiRoutesService.removeFirstPlaneRoute(req,res));
				
				
			// ************

			
			// *** CITIES ***
				//Fetch ALL cities
				// E.G. curl -X GET http://localhost:5000/cities
				get("/cities",(req,res) -> apiRoutesService.getCities(req,res));
			
				//Fetch City by ID
				// E.G. curl -X GET http://localhost:5000/cities/London
				get("/cities/:id",(req,res) -> apiRoutesService.getCityById(req,res));
				
				get("/cities/:id/neighbors/:count", (req, res) -> apiRoutesService.getNearbyCitiesForId(req,res));
			// ************

			
			// *** CARGO ***
			// ************
				// Create a new cargo at "location" which needs to get to "destination" - error if neither location nor destination exist as cities. Set status to "in progress" 
				// E.G. curl -X POST http://localhost:5000/cargo/London/to/Cairo
				post("/cargo/:location/to/:destination",(req,res) -> apiRoutesService.createCargo(req,res));
				
				//Fetch Cargo by ID
				// E.G. curl -X GET http://localhost:5000/cargo/location/London
				get("/cargo/location/:location",(req,res) -> apiRoutesService.getCargoAtLocation(req,res));

				// Set status field to 'Delivered' - Increment some count of delivered items too.
				// E.G. curl -X PUT http://localhost:5000/cargo/5f45303156fd8ce208650caf/delivered
				put("/cargo/:id/delivered",(req,res) -> apiRoutesService.cargoDelivered(req,res));

				// Mark that the next time the courier (plane) arrives at the location of this package it should be onloaded by setting the courier field - courier should be a plane.
				// E.G. curl -X PUT http://localhost:5000/cargo/5f45303156fd8ce208650caf/courier/CARGO10
				put("/cargo/:id/courier/:courier",(req,res) -> apiRoutesService.cargoAssignCourier(req,res));

				// Unset the value of courier on a given piece of cargo
				// E.G. curl -X DELETE http://localhost:5000/cargo/5f4530d756fd8ce208650d83/courier
				delete("/cargo/:id/courier",(req,res) -> apiRoutesService.cargoUnsetCourier(req,res));

				// Move a piece of cargo from one location to another (plane to city or vice-versa)
				// E.G. curl -X PUT http://localhost:5000/cargo/5f4530d756fd8ce208650d83/location/London
				put("/cargo/:id/location/:location",(req,res) -> apiRoutesService.cargoMove(req,res));

				
				get("/planes/history/:id",(req,res) -> apiRoutesService.getPlanesHistoryRecords(req,res));
				
				
			after((req, res) -> {
				res.type("application/json");
			});
		

		//return;
	}

}
