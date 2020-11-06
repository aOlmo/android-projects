import numpy as np
from model import compute, get_db_dict
from collections import deque

N = 11

# https://codereview.stackexchange.com/questions/148399/graph-and-node-classes-with-bfs-and-dfs-functions
class Node:
    def __init__(self, val, date):
        self.val = val
        self.date = date
        self.edges = []

    def __eq__(self, other):
        return self.date == other.date and self.val == other.val

    def __hash__(self):
        return self.val

    def __repr__(self):
        return "{}: {}".format(self.val, self.date[:8])


class Graph:
    def __init__(self, nodes=[]):
        self.nodes = nodes
        self.adj_mat = np.zeros((N, N))
        self.db_dict = get_db_dict(N)

    # Val here means the starting date
    def add_node(self, val, date):
        new_node = Node(val, date)
        self.nodes.append(new_node)
        return new_node

    def add_edge(self, node1, node2):
        node1.edges.append(node2)
        # node2.edges.append(node1)

    def bfs(self):
        if not self.nodes:
            return []
        start = self.nodes[0]

        visited, queue, result = set([start]), deque([start]), []
        while queue:
            node = queue.popleft()
            result.append(node)
            if(not node.edges): self.do_computation(node)
            for nd in node.edges:
                if nd not in visited:
                    queue.append(nd)
                    self.do_computation(nd)
                    visited.add(nd)

        print(self.adj_mat)
        return result

    def do_computation(self, root):
        compute(self, root)

graph = Graph()
graph.add_node(1, "20110614")

bfs_result = graph.bfs()


# parser = argparse.ArgumentParser(description='Pass the id and date of a person and get its adjacency graph for '
#                                                  'the past 7 days before and 5km radious')
# parser.add_argument('--id', type=int, help='ID of the person')
# parser.add_argument('--date', help='Date to start looking for')
#
# args = parser.parse_args()
# id = args.id
# date = args.date
# # date_to_epoch()