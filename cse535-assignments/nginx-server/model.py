#!/usr/bin/env python3
import sqlite3
import math
import pandas as pd
import numpy as np
import time
import argparse

from datetime import datetime, timedelta

# Time window to check for close coordinates.
N_SECS = 60
DIST = 5000  # In meters
N_DAYS = 8

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


def get_db_dict(n):
    col_names = ['_latitude', '_longitude', '_time_location']
    fd = {}
    for i in range(n):
        db = "db/LifeMap_GS{}.db".format(i + 1)
        cursor = sqlite3.connect(db).cursor()

        cursor.execute("SELECT {} FROM locationTable".format(",".join(col_names)))
        col_content = np.array(cursor.fetchall())

        locTableData = pd.DataFrame(data=col_content.T, index=col_names).T
        fd[i] = locTableData.set_index("_time_location").to_dict("index")
    return fd

def preproc_person_dict(d, dates, str_starting_date):
    preproc_dict = {key: value for key, value in d.items() if key.startswith(tuple(dates))}
    if len(preproc_dict) == 0:
        # print("[-]: No days found for person {} and starting date {} ".format(sel_id, str_starting_date))
        return False
    return preproc_dict


# 5km and 7 days
def compute(graph, root):
    fd = graph.db_dict
    sel_date = root.date  # Selected date
    sel_id = root.val
    adj_mat = graph.adj_mat
    print("=========================================")
    print("[+]: COMPUTING FOR NODE {} with time {}".format(sel_id, sel_date[:8]))
    print("=========================================")

    Y, M, d = int(sel_date[:4]), int(sel_date[4:6]), int(sel_date[6:8])
    starting_date = datetime(year=Y, month=M, day=d)
    str_starting_date = starting_date.strftime("%Y%m%d")

    # Get the range of days to process from each person
    sel_dates = []
    for i in range(N_DAYS):
        new_date = starting_date - timedelta(days=i)
        sel_dates.append(new_date.strftime("%Y%m%d"))

    sel_person = fd[sel_id]
    sel_person = preproc_person_dict(sel_person, sel_dates, str_starting_date)

    for cur_id in fd:
        if cur_id == sel_id: continue
        print("[+]: Processing {}-{}".format(sel_id, cur_id))

        cur_person = fd[cur_id]
        cur_person = preproc_person_dict(cur_person, sel_dates, str_starting_date)
        if not cur_person: continue

        for cur_date, cur_pos in cur_person.items():
            if adj_mat[sel_id, cur_id] or adj_mat[cur_id, sel_id]: break
            cur_lat, cur_lon = float(cur_pos["_latitude"]), float(cur_pos["_longitude"])

            for sel_date, sel_pos in sel_person.items():
                sel_lat, sel_lon = float(sel_pos["_latitude"]), float(sel_pos["_longitude"])

                dist = harvesine_distance(cur_lat / 1e6, sel_lat / 1e6, cur_lon / 1e6, sel_lon / 1e6)

                err_flag = cur_lat + sel_lat + cur_lon + sel_lon
                if dist <= DIST and err_flag != 0.0:
                    print("[+]: {} and {} | dist {:.2f}m times {}/{}".format(sel_id, cur_id, dist, sel_date, cur_date))
                    adj_mat[sel_id, cur_id] = 1
                    adj_mat[cur_id, sel_id] = 1

                    new_node = graph.add_node(cur_id, cur_date)
                    graph.add_edge(root, new_node)
                    print("[+]: Putting {} as new node to process".format(cur_id))
                    break


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
