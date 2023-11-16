import os
import pandas as pd
import sys
from pyvis.network import Network

if __name__ == "__main__":
    workingDir = sys.argv[1]
    exportFile = sys.argv[2]
    nodesFile = sys.argv[3]
    edgesFile = sys.argv[4]

os.chdir(workingDir)

net = Network(directed=True) #, filter_menu=True

nodes = pd.read_csv(nodesFile, na_filter=False)
edges = pd.read_csv(edgesFile, na_filter=False)

node_ids = nodes['id']
node_labels = nodes['label']
node_titles = nodes['title']
node_shapes = nodes['shape']
node_border_widths = nodes['borderWidth']
node_colors = nodes['color']
node_sizes = nodes['size']

edge_sources = edges['source']
edge_targets = edges['target']
edge_titles = edges['title']
edge_weights = edges['weight']
edge_widths = edges['width']

node_data = zip(node_ids, node_labels, node_titles, node_shapes, node_border_widths, node_colors, node_sizes)
edge_data = zip(edge_sources, edge_targets, edge_titles, edge_weights, edge_widths)

for node in node_data:
    id = node[0]
    label = node[1]
    title = node[2]
    shape = node[3]
    border_width = node[4]
    color = node[5]
    size = node[6]

    if not shape:
        net.add_node(id, label=label, title=title, borderWidth=border_width, color=color, size=size)
    else:
        net.add_node(id, label=label, title=title, shape=shape, borderWidth=border_width, color=color, size=size)

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

net.show(exportFile)
