from pyvis.network import Network
import pandas as pd
import sys, os

if __name__ == "__main__":
    workingDir = sys.argv[1]
    exportFile = sys.argv[2]
    nodesFile = sys.argv[3]
    edgesFile = sys.argv[4]

os.chdir(workingDir)

net = Network(directed=True) #, filter_menu=True

print('Read nodes CSV file')
nodes = pd.read_csv(nodesFile, na_filter=False)
print('Read edges CSV file')
edges = pd.read_csv(edgesFile, na_filter=False)

node_ids = nodes['id']
node_labels = nodes['label']
node_titles = nodes['title']
node_shapes = nodes['shape']
node_colors = nodes['color']
node_sizes = nodes['size']

edge_sources = edges['source']
edge_targets = edges['target']
edge_titles = edges['title']
edge_weights = edges['weight']
edge_widths = edges['width']

node_data = zip(node_ids, node_labels, node_titles, node_shapes, node_colors, node_sizes)
edge_data = zip(edge_sources, edge_targets, edge_titles, edge_weights, edge_widths)

print('Add nodes to network')
for node in node_data:
    id = node[0]
    label = node[1]
    title = node[2]
    shape = node[3]
    color = node[4]
    size = node[5]

    net.add_node(id, label=label, title=title, shape=shape, color=color, size=size)

print('Add edges to network')
for edge in edge_data:
    source = edge[0]
    target = edge[1]
    title = edge[2]
    weight = edge[3]
    width = edge[4]

    if not title:
        net.add_edge(source, target, weight=weight, width=width)
    else:
        net.add_edge(source, target, title=title, weight=weight, width=width)

net.set_options('''
  var options = {
    "configure": {
      "enabled": false,
      "filter": ["physics"]
    },
    "nodes": {
      "font": {
        "size": 18,
        "strokeWidth": 3
      }
    },
    "edges": {
      "arrowStrikethrough": false,
      "smooth": false
    },
    "physics": {
      "barnesHut": {
        "gravitationalConstant": -15000,
        "centralGravity": 0.25,
        "springLength": 105,
        "springConstant": 0.095,
        "damping": 0.125,
        "avoidOverlap": 0.95
      }
    }
  }
''')

print('Generate HTML file')
net.show(exportFile)
