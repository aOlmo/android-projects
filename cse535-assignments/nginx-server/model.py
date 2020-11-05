#!/usr/bin/env python3
import sqlite3
import math
import pandas as pd
import numpy as np
import time

from datetime import datetime

# Time window to check for close coordinates.
N_SECS = 30
DIST = 3  # In meters

# https://www.movable-type.co.uk/scripts/latlong.html
# Calculates the Harvesine distance passing the lat and lon values with 1e-6 precision
def harvesine_distance(lat1, lat2, lon1, lon2):
    R = 6371e3  # Earth Radius in metres
    g1 = lat1 * math.pi / 180.
    g2 = lat2 * math.pi / 180.

    dg = (lat2 - lat1) * math.pi / 180.
    dl = (lon2 - lon1) * math.pi / 180.

    a = math.sin(dg / 2) * math.sin(dg / 2) + \
        math.cos(g1) * math.cos(g2) * \
        math.sin(dl / 2) * math.sin(dl / 2)

    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    d = R * c  # distance in between in metres
    return d


if __name__ == '__main__':
    col_names = ['_latitude', '_longitude', '_time_location']
    n = 12
    fd = {}
    for i in range(n):
        db = "db/LifeMap_GS{}.db".format(i + 1)
        cursor = sqlite3.connect(db).cursor()

        cursor.execute("SELECT {} FROM locationTable".format(",".join(col_names)))
        col_content = np.array(cursor.fetchall())

        # col_content[:, 2] = np.array(list(map(trim, col_content[:, 2])))
        locTableData = pd.DataFrame(data=col_content.T, index=col_names).T
        fd[i] = locTableData.set_index("_time_location").to_dict("index")

    start = time.time()
    adj_mat = np.identity(n)
    c = 0
    for i in range(n):
        cur = fd[i]
        for j in range(i, n):
            aux = fd[j]
            #print("[+]: Doing {}-{}".format(i, j))
            for key in cur.keys():
                if i == j or adj_mat[i, j] == 1:
                    continue
                Y, M, d = int(key[:4]), int(key[4:6]), int(key[6:8])
                H, m, s = int(key[8:10]), int(key[10:12]), int(key[12:14])

                cur_time = datetime(Y, M, d, H, m, s).timestamp()
                for sec in range(-N_SECS // 2, N_SECS // 2):
                    next_time = datetime.fromtimestamp(cur_time + sec)
                    next_time = next_time.strftime("%Y%m%d%H%M%S%a").upper()
                    if next_time in aux.keys():
                        cur_lat, cur_lon = float(cur[key]["_latitude"]), float(cur[key]["_longitude"])
                        aux_lat, aux_lon = float(aux[next_time]["_latitude"]), float(aux[next_time]["_longitude"])
                        # dist = math.sqrt((cur_lat - aux_lat) ** 2 + (cur_lon - aux_lon) ** 2)
                        dist = harvesine_distance(cur_lat / 1e6, aux_lat / 1e6, cur_lon / 1e6, aux_lon / 1e6)
                        flag = cur_lat + aux_lat + cur_lon + aux_lon
                        if dist <= DIST and flag != 0.0:
                            #print("Dist={:.2f} | {} and {} | {}/{} and {}/{}".format(dist, i, j, cur_lat, aux_lat,
                            print("Found {} and {} at a distance of {:.2f}m at time {}".format(i, j, dist, next_time))
                            adj_mat[i, j] = 1
                            adj_mat[j, i] = 1
                            break
    print("[+]: Processing took {:.2f}s \n\n\n".format(time.time() - start))
    print("-------- Adjacency matrix --------")
    print(adj_mat)

#########################################################

# print(harvesine_distance(37.561812, 37.561822, 126.935396, 126.935386)) # 1.419

# cursor.execute("PRAGMA table_info([locationTable])")
# col_names = np.array(cursor.fetchall())[:, 1]

# if i==5 and j==7:
#     print(123)

# if i == 5:
#     col_content = np.vstack((col_content, np.array(["37561812", "126935396", "20111115101115TUE"])))
#
# if i == 7:
#     col_content = np.vstack((col_content, np.array(["37561822", "126935386", "20111115101115TUE"])))
