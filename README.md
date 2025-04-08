this was an extension of a school project for CS 2110

# Magic Scissors

Magic Scissors is an interactive image selection tool that lets users extract objects from images by clicking around their outlines. Powered by Dijkstra's algorithm and a custom priority queue, this project brings smart, path-based image segmentation to the web.

##  Features

- Click-based polygonal selection on images
- Real-time pathfinding using Dijkstra’s algorithm
- Custom-built priority queue for optimized performance
- Full-stack implementation (optional: specify backend if used)
- Preview and extract selected object regions

## How It Works

1. User uploads or loads an image.
2. They click around the object they want to extract.
3. The system runs Dijkstra’s algorithm to trace the best path between points based on image edge weights.
4. A polygonal selection is drawn in real time, simulating intelligent scissors.
