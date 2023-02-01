import sys
from os import path, access, R_OK
import networkx as nx
from networkx.classes.digraph import DiGraph
from networkx.generators.trees import NIL

paths = [[]]

def usage():
    sys.stdout.write("path_generator.py by Dong WANG \n")
    sys.stdout.write("    Generate all paths from .dot file created by TLC checking output\n\n")
    sys.stdout.write("USAGE: path_generator.py ROOT_NODE /path/to/file.dot /path/to/store/paths\n")

def findRoot(diGraph):
    # G=nx.balanced_tree(2,3,create_using=diGraph)
    # for n,d in G.in_degree():
    #     if d==0:
    #         return n
    return nx.topological_sort(diGraph)[0]

# Construct a path by edge-based traverse
def iterate(diGraph, n, rowId, label):
    global paths
    row = paths[rowId]
    if 'Leader' in label[n]:
        return
    succ = diGraph.successors(n)
    first_succ = None
    # if n == '-2413802876780086654':
    #     for node in succ:
    #         print('Debug:', node)
    #print('begin.')
    for node in succ:
        # We do not consider slef-loop
        if(node == n):
            continue
        repeatable_edge = False
        # Single hop circle
        #if node in diGraph.predecessors(n):
        #    repeatable_edge = True

        # Skip the edge if it already exists in the path
        if node in row:
            i = 0
            for existing_node in row:
                # if(len(row) > 24) and i > 23:
                #     print(i, row[row.index(existing_node,1) - 1], existing_node, n, node)
                if i > 1 and (existing_node == node) and (row[i - 1] == n):
                    #print("!!!!!!!!REPETABLE!!!!!!!!!!!",n,existing_node,i)
                    repeatable_edge = True
                    break
                i=i+1
        if not repeatable_edge:
            # If it is the first successor, we directly
            # add it in the path
            if first_succ == None:
                #print("New node:", node, "At ", len(row), "Pre", n)
                first_succ = node
                row.append(node)
            # If it is not the first, we genearte a new
            # path
            else:
                #print("Add new path for node:", node)
                path_num = len(paths)
                new_row = row.copy()
                new_row.pop() # Remove first_succ
                paths.append(new_row)
                paths[path_num].append(node)
    # After getting over all succs, continue traverse
    # on the first successor
    if first_succ != None:
        iterate(diGraph, first_succ, rowId, label)

def output(graph, label, lists, output):
    node_file = open(output+'ep.node','w')
    edge_file = open(output+'ep.edge','w')
    for i, node in enumerate(graph):
        if node.isdigit() or node.startswith('-'):
            node_file.write(node + ' ' + label[node] + '\n')
    for path in paths:
        for i, node in enumerate(path):
            if i == 0:
                edge_file.write(node + ' ')
            else:
                behavior = graph[path[i-1]][node]['label']
                print(behavior)
                print(i, node)
                edge_file.write(behavior + ' ' + node + ' ')
        edge_file.write('\n')


def main():
    if (len(sys.argv) > 3):
        root = sys.argv[1]
        path = sys.argv[2]
        dir = sys.argv[3]
    else:
        usage()
        sys.exit(1)
    
    try:
        fh = open(path)
    except IOError as e:
        sys.stderr.write("ERROR: could not read file" + path + "\n")
        usage()
        sys.exit(1)

    G = nx.DiGraph(nx.drawing.nx_agraph.read_dot(path))
    print("Successfully read graph file.")
    n = nx.classes.function.number_of_nodes(G)
    e = nx.classes.function.number_of_edges(G)
    label = nx.get_node_attributes(G, 'label')
    # e_label = nx.get_edge_attributes(G, 'label')

    print("The graph contains", n, "nodes and", e, "edges.")

    # root = findRoot(G)
    # print(label['-6798421928611449998'])
    # print(len(list(nx.simple_cycles(G))))
    paths[0].append(root)
    iterate(G, root, 0, label)
    path_num = len(paths)
    print('[1]path num:', path_num, len(paths[0]))
    # is_new_path = True
    # while is_new_path:
    #     print('path num:', path_num)
    #     for row in paths:
    #         iterate(G, row[0], paths.index(row))
    #     new_path_num = len(paths)
    #     is_new_path = (path_num == is_new_path)
    #     path_num = new_path_num

    # for i in paths[0]:
    #     print(i, end = ' ')

    # for i in paths:
    #     for j in i:
    #         print(j, end=' ')
    #     print()

    # for i, row in enumerate(paths):
    #     node_num = len(row)
    #     iterate(G, row[node_num - 1], i, label)
    #     if i > 100:
    #         break

    #iterate(G, paths[1][len(paths[1])-1], 1)
    # print('[2]path num:', len(paths))

    output(G, label, paths, dir)
    # for i in paths:
    #     for j in i:
    #         print(j, end=' ')
    #     print()
main()
