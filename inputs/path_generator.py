import sys
import random
import time
import networkx as nx
from networkx.classes.digraph import DiGraph

PATHS = [[]]

def usage():
    sys.stdout.write("path_generator.py by Dong WANG \n")
    sys.stdout.write("    Generate all paths from a .dot file created by TLC checking output\n\n")
    sys.stdout.write("USAGE: path_generator.py END_ACTION /path/to/file.dot /path/to/store/paths\n [POR]")

##################################### General functions #########################################

"""
Find the root node for a state space graph

Parameters:
    diGraph: the state space networkx graph

Returns:
    node: the root node (initial state)
"""
def find_root(diGraph):
    node = None
    for n in diGraph.nodes(data=True):
        predecessors = diGraph.predecessors(n[0])
        if len(list(predecessors)) == 0:
            node = n
            break
    return node

"""
Output paths to files, in which '.node' file stores maps of 
state ID and contents, and '.edge' file stores paths.

Parameters:
    graph: the state space networkx graph
    output: the directory to store path files
Return:
    None
"""
def output(graph, output):
    label = nx.get_node_attributes(graph, 'label')
    node_file = open(output+'.node','w')
    edge_file = open(output+'.edge','w')
    # Write all node information
    for i, node in enumerate(graph):
        if node.isdigit() or node.startswith('-'):
            node_file.write(node + ' ' + label[node] + '\n')
    # Write all edge information
    for path in PATHS:
        for i, node in enumerate(path):
            if i == 0:
                edge_file.write(node + ' ')
            else:
                action = graph[path[i-1]][node]['label']
                edge_file.write(action + ' ' + node + ' ')
        edge_file.write('\n')

##################################### Basic Mocket functions #########################################

"""
Add labels for each edge in the state space graph.

Parameters:
    diGraph: the state space networkx graph

Returns:
    None
"""
def add_visit_label(diGraph):
    edges = nx.edge_dfs(diGraph)
    for edge in edges:
        # For edge coverage guided traversal
        diGraph[edge[0]][edge[1]]['visited'] = False
        # For partial order reduction
        diGraph[edge[0]][edge[1]]['por'] = False

"""
Partial order reduction. 
Add labels for edges in the rhombus structure. For two
commutative edges, we randomly choose one schedule and
set the label of edges in this schedule as equivalent,
and the labeled edges will not be added in any path in
traversal.

Specifically, we traverse every node to its grandchild
nodes to find these rhombuses.
     n0
    /  \ 
   e1   e2 
  /      \ 
n1        n2
  \      /  \ 
   e2   e1   e3
    \  /
     n3
Assume a rhombuse as above, we mark < n0 -> n1, n1 -> n3> 
or < n0 -> n2, n2 -> n3 > as equivalent. Note that n2 has
another grandchild. Thus, if we choose to set the path of 
n2 as equivalent, we should only skip n0 -> n2, and leave 
n2 -> n3 to cover e3.
     
Parameters:
    diGraph: the state space networkx graph

Returns:
    None
"""
def add_POR_label(diGraph):
    left_or_right = ["L", "R"]
    for n in diGraph.nodes(data=True):
        top_node = n[0]
        left_edge = None
        for left_node in diGraph.successors(top_node):
            left_edge = diGraph[top_node][left_node]['label']
            for right_node in diGraph.successors(top_node):
                if left_node != right_node:
                    right_edge = diGraph[top_node][right_node]['label']
                    for btmNode in diGraph.successors(left_node):
                        if diGraph.has_edge(right_node, btmNode) and \
                            diGraph[left_node][btmNode]['label'] == right_edge and \
                            diGraph[right_node][btmNode]['label'] == left_edge:
                            # If these two paths has already been labeled, skip them.
                            if diGraph[right_node][btmNode]['por'] == True or \
                               diGraph[left_node][btmNode]['por'] == True:
                                continue
                            # Randomly choose right path
                            elif random.choice(left_or_right) == "R":
                                diGraph[left_node][btmNode]['por'] = True
                                if diGraph.out_degree(left_node) == 1:
                                    diGraph[top_node][left_node]['por'] = True
                            # Randomly choose left path
                            else:
                                diGraph[right_node][btmNode]['por'] = True
                                if diGraph.out_degree(right_node) == 1:
                                    diGraph[top_node][right_node]['por'] = True

"""
Construct a path by edge-based traversal.
A path struct: (StateID1, StateID2, ...)

Parameters:
    diGraph: the state space networkx graph
    preNode: the root node
    curNode: current state
    end: end action name
    pathID: current path to generate
    POR: whether enable partial order reduction

Returns:
    None
"""
def traverse(diGraph, preNode, curNode, endAction, pathID, POR):
    global PATHS
    isEndState = False
    isAllOutEdgesVisited = True
    if(preNode != None): # Current node is not initial state
        actionName = diGraph[preNode][curNode]['label']
        if(actionName == endAction):
            #print('Is end state:', curNode)
            isEndState = True
    for succ in diGraph.successors(curNode):
        if(diGraph[curNode][succ]['visited'] == False):
            if (not POR) or (diGraph[curNode][succ]['por'] == False):
                isAllOutEdgesVisited = False
                break
    if(isEndState or isAllOutEdgesVisited):
        return
    first_succ = None
    for succ in diGraph.successors(curNode):
        if(diGraph[curNode][succ]['visited'] == True) or (POR and (diGraph[curNode][succ]['por'] == True)):
            #print('Is visited edge:',curNode, succ)
            continue
        else:
            diGraph[curNode][succ]['visited'] = True
            # If it is the first successor, we directly
            # add it in the path
            if first_succ == None:
                first_succ = succ
                PATHS[pathID].append(succ)
                #print('Add new node:', succ)
            # If it is not the first, we generate a new
            # path for later traversal
            else:
                path_num = len(PATHS)
                new_path = PATHS[pathID].copy()
                new_path.pop()
                PATHS.append(new_path)
                PATHS[path_num].append(succ)
                #print('Add new path for:', succ)
    if first_succ != None:
        #print('Traverse at:', first_succ)
        traverse(diGraph, curNode, first_succ, endAction, pathID, POR)

"""
Main function
"""
def main():
    if (len(sys.argv) > 3):
        end = sys.argv[1]
        path = sys.argv[2]
        dir = sys.argv[3]
        POR = False
        if (len(sys.argv) == 5):
            if (sys.argv[4].lower() == 'por'):
                POR = True
    else:
        usage()
        sys.exit(1)
    
    try:
        open(path)
    except IOError as e:
        sys.stderr.write("ERROR: could not read file" + path + "\n")
        usage()
        sys.exit(1)

    start_time = time.time()
    G = nx.DiGraph(nx.drawing.nx_agraph.read_dot(path))
    print("Successfully read graph file.")

    n = nx.DiGraph.number_of_nodes(G)
    e = nx.DiGraph.number_of_edges(G)
    print("The graph contains", n, "nodes and", e, "edges.")

    root = find_root(G)
    print("Find the root node:", root[0])

    add_visit_label(G)
    add_POR_label(G)

    PATHS[0].append(root[0])
    traverse(G, None, root[0], end, 0, POR)
    path_num = len(PATHS)
    print('path[ 0 ]:', 'path num:', path_num, ',length', len(PATHS[0]))

    cur_path_ID = 1
    cur_path_num = len(PATHS)
    while cur_path_ID < cur_path_num:
        cur_path = PATHS[cur_path_ID]
        length = len(cur_path)
        if length > 1: # The path should at least contain an initial state and current state
            traverse(G, cur_path[length-2], cur_path[length-1], end, cur_path_ID, POR)
        else:
            sys.stderr.write("ERROR! Wrong traversal for current path!")
        cur_path_num = len(PATHS)
        print('path[', cur_path_ID, '],', 'path num:', cur_path_num, ',length', len(PATHS[cur_path_ID]))
        cur_path_ID = cur_path_ID + 1

    output(G, dir)
    print("--- %s seconds spent---" % (time.time() - start_time))

main()
