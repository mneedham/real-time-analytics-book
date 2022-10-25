import numpy as np
import random
from faker import Faker
import csv
from shapely.geometry import Polygon, Point

poly = Polygon([
    #Polygon_0
    (13.068435398495534, 77.49105088969723),
    (13.068435398495534, 77.49105088969723),
    (12.990166178540756, 77.47525804301755),
    (12.911872285691553, 77.48143785258786),
    (12.875058978354334, 77.5164567734863),
    (12.8596627183304, 77.60778062602536),
    (12.901163518471398, 77.7087175156738),
    (12.960056055140743, 77.72245042583005),
    (12.984813534241871, 77.75334947368161),
    (13.013582640465126, 77.7251970078613),
    (13.060408925722843, 77.64760606547848),
    (13.08314992060076, 77.65859239360348),
    (13.102879378644063, 77.61258714458005),
    (13.091844452767342, 77.55559556743161)

])


def polygon_random_points(poly, num_points):
    min_x, min_y, max_x, max_y = poly.bounds
    points = []
    while len(points) < num_points:
        random_point = Point([random.uniform(min_x, max_x), random.uniform(min_y, max_y)])
        if (random_point.within(poly)):
            points.append(random_point)
    return points


points = polygon_random_points(poly, 10_000)
fake = Faker(['en_IN'])


with open("users.csv", "w") as users_file:
    writer = csv.writer(users_file, delimiter=",")
    writer.writerow(["firstName", "lastName", "email", "residence", "lat", "lon"])
    for p in points:
        profile = fake.profile()
        writer.writerow([
            profile['name'].split(" ")[0],
            profile['name'].split(" ")[1],
            profile['mail'],
            profile['residence'].replace('\n', ', '),
            p.x, p.y
        ])
        print(profile)

