from pyproj import Geod
import geopy.distance

geoid = Geod(ellps="WGS84")

point1 = (12.978268132410502, 77.59408889388118)
point2 = (13.010025895408063,77.62104744796308)

km_per_hour = 150

dist = geopy.distance.distance(point1, point2).meters
print("Distance", dist)

minutes_to_deliver = (dist / (km_per_hour * 1000)) * 60
print("Time to deliver", minutes_to_deliver)


points_to_generate = minutes_to_deliver * 12

extra_points = geoid.npts(point1[0], point1[1], point2[0], point2[1], points_to_generate)
for point in extra_points:
    print(point)