#!/usr/bin/python3
import requests 
import json
import datetime
import math
import unit_tests
import time
from pprint import pprint
import random

city_locations = {}

#This is also in unit_tests

url = "http://localhost:5001"
delivered_count = 0

def run_unit_tests():
    unit_tests.city_unit_tests()
    unit_tests.plane_unit_tests()
    unit_tests.cargo_unit_tests()


#Land when we get close to an airport
# Unload and deliver anythign bound for here
# Keep anything bound for somethere on our route
# Unload and unown anything else

def LandPlane(plane,destination,destLocation):
    global delivered_count
    #print('Landing %s at %s'  % (plane['callsign'],destination))
    plane['currentLocation']=destLocation
    plane['heading']=0
    
      
    requests.delete("%s/planes/%s/route/destination" % (url,plane['callsign']))
    requests.put( "%s/planes/%s/location/%f,%f/%d/%s"  % (url,plane['callsign'],
    plane['currentLocation'][0], plane['currentLocation'][1],plane['heading'],destination))

    cargoes =  requests.get("%s/cargo/location/%s" % (url,plane['callsign'])).json()

    #Unload everything when we land - and remove it's courier unless it's headed to somewhere we 
    #are routed to
   
    if  cargoes != None :
        for cargo in cargoes:
            if cargo['destination'] == destination:
                print("Delivered package %s" % (cargo['id']))
                delivered_count = delivered_count + 1
                requests.put("%s/cargo/%s/location/%s" % (url,cargo['id'],destination))
                requests.put("%s/cargo/%s/delivered" % (url,cargo['id']))
                
            else:
                if cargo['destination'] in plane['route'] :
                    pass
                    #print("Keeping cargo %s onboard for delivery to %s" % (cargo['id'],cargo['destination']))
                else:
                    requests.delete("%s/cargo/%s/courier" % (url,cargo['id']))
                    requests.put("%s/cargo/%s/location/%s" % (url,cargo['id'],destination))
                    #print("Offloading %s" % cargo['id'])


    #Flag as delivered if this is the final destination
    #Mark as uncouriered 
    #Take on any Cargo for me
    sitecargo =  requests.get("%s/cargo/location/%s" % (url,destination)).json()

    if sitecargo != None:
        for cargo in sitecargo:
            if cargo['courier'] == plane['callsign']:
                #print("On Loading %s for %s" % (cargo['id'],cargo['destination']))
                requests.put("%s/cargo/%s/location/%s" % (url,cargo['id'], plane['callsign']))


def roundtwo(n):
    return int(n*100)/100;

def movePlane(plane,destination):

    if destination == None:
         return
    currentLocation = plane['currentLocation']
           
    destLocation = city_locations[destination]
    #print(`${plane.callsign} ${plane.currentLocation} going to ${destination} ${destLocation}`);

    dx = destLocation[0]-currentLocation[0]
    dy = destLocation[1]-currentLocation[1]

    if abs(dx) < 0.5 and abs(dy) < 0.5:
       #console.log(`${plane.callsign} ${plane.currentLocation} going to ${destination} ${destLocation}`);
       LandPlane(plane,destination,destLocation)
    else:
        if dy == 0:
            dy = 0.00000001
        angle = math.atan(dx/dy)*180/math.pi
                            
        if dy < 0:
             angle = angle  + 180
        if angle < 0:
            angle = angle + 360
        #Calculate new position*
        dLon = math.sin(angle*math.pi/180)
        dLat = math.cos(angle*math.pi/180)
        newval = [roundtwo(plane['currentLocation'][0]+dLon),roundtwo(plane['currentLocation'][1]+dLat)];
        plane['currentLocation']=newval
        plane['heading'] = math.floor(angle);
        #TODO Teach planes the world is round :0)
        r = requests.put("%s/planes/%s/location/%f,%f/%d" % (url,plane['callsign'],plane['currentLocation'][0],
        plane['currentLocation'][1],plane['heading']))
 
 #This is somewhat expensive but infrequent (every few minutes)
def allocate_route(plane):
    pickuplength = 10
    route = []
    callsign = plane.get("callsign")
    #print(f"Plane {callsign} is idle - Tasking")
    #Send the plane to a buncn of nearby cities to collect all the parcels then deliver them
    first_city = random.choice(list(city_locations.keys())) 
    #print(f"Sending it to {first_city} to pick up")
    route.append(first_city)
    requests.post(url + "/planes/" + callsign + "/route/" + first_city )
    neighbor_cities = requests.get(url + "/cities/" + first_city + "/neighbors/"+str(pickuplength)).json()
    #print(neighbor_cities)
    for c in  neighbor_cities.get('neighbors'):  
        prev_city = c
        route.append(c.get('name'))
        #print(f"Then { c.get('name')}")
        requests.post(url + "/planes/" + callsign + "/route/" + c.get('name') )
    
    #Now for each package on route, allocate it to this plane and add the dest   ination to a list of
    #destinations
    deliverlocations = []   
    for pickup in route:
        #print(f"Checking for parcels in {pickup}")
        parcels =  requests.get(url + "/cargo/location/" + pickup ).json()
        for parcel in parcels:
            #print(parcel)
            parcelid = parcel.get("id")
            rval = requests.put(url+"/cargo/"+parcelid+"/courier/"+callsign)
            deliverlocations.append(parcel.get("destination"))
    #print(f"Delivering to {deliverlocations}")
    #find the closet in that set , we will go there first, then the closest ot that and so on.
    #We have co-ordinates so let's do that client side here
    delivercities = []
    for dl in set(deliverlocations):
        delivercities.append(requests.get(url + "/cities/" + dl).json())
    #print(delivercities)
    #print("Flying from "+prev_city.get("name"))
    while len(delivercities) > 0:
        nearest = sorted( delivercities , key = lambda l : distance (l,prev_city)  )
        next_city = nearest[0]
        #print("then to "+next_city.get("name"))
        requests.post(url + "/planes/" + callsign + "/route/" + next_city.get("name") )
        delivercities = nearest[1:]
        prev_city=next_city
                                
 
 
def distance(tocity,fromcity):
    dx = (tocity.get("location")[0] - fromcity.get("location")[0]  )
    dy = (tocity.get("location")[1] - fromcity.get("location")[1]  )
    return (dx*dx)+(dy*dy) 


def run_simulation():
    
    #Move planes and set heading
    cities = requests.get(url + "/cities").json()
    for city in cities:
        city_locations[city['name']] = city['location']

    while True:
        planes = requests.get(url + "/planes").json()
        for plane in planes:
            if len(plane["route"]) > 0:
                destination = plane["route"][0]
                movePlane(plane,destination)
            else:
                allocate_route(plane)


        #New cargo arrives randomly
        arrive_at = random.choice(list(city_locations.keys()))
        send_to = random.choice(list(city_locations.keys()))

        if arrive_at != send_to:
            cargoes =  requests.get("%s/cargo/location/%s" % (url,arrive_at)).json()
            
            if len(cargoes) < 20 :
                    print("Parcel arrives at %s  to deliver to  %s, (%d waiting)  " % (arrive_at,send_to,len(cargoes)+1))
                    requests.post("%s/cargo/%s/to/%s" % (url,arrive_at,send_to))
        #time.sleep(0.5)

        print(f" { delivered_count*60 /   ( int(time.time())-start_time)} parcels per minute ")
        


#Simple algorithm  - each plan gets a stadard route it flies round
#Each plane will wil allocated a random starting point, it will then fly to cities near there collecting all the 
#packages and adding their destination to it's route and then it will fly to those places in reverse distance order


if __name__ == "__main__": 
   #run_unit_tests()
   start_time =int(time.time())
   time.sleep(1)
   run_simulation()
