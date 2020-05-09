import matplotlib.pyplot as plt
import pandas as pd
from matplotlib.animation import FuncAnimation
import numpy as np

y1 = []
y2 = []
y3 = []
def animate(i):
   data = pd.read_csv('data.csv')
   y1 = data['well']
   y2 = data['recovered']
   y3 = data['infected']
   labels = ["recovered ", "well", "infected"]
   color_map = ["#9ACD32", "#DFE9E4", "#E43C31"]
   bar = np.add(y1,y2).tolist()
   plt.cla()
   axes = plt.gca()
   axes.set_xlim(0, 1000)
   axes.set_ylim(0, 50)
   #plt.gca().set_facecolor('xkcd:gray')
   plt.gca().invert_yaxis()
   row = data.index.values
   col = data.to_numpy()
   #plt.bar(row, y2, color='green', edgecolor='white', width=0.3)
   #plt.bar(row, y1, bottom = y2, color='gray', edgecolor='white', width=0.3)
   #plt.bar(row, y3, bottom = bar, color='red', edgecolor='white', width=0.3)
   plt.stackplot(row,y2,y1,y3, labels=labels,colors = color_map)
   '''
   plt.fill_between(row, col[:,0], 0,
         facecolor="orange", # The fill color
         color='white',       # The outline color
         alpha=0.8)         # Transparency of the fill
    
   plt.fill_between(row, col[:,1],0,
         facecolor="green", # The fill color
         color='green',     # The outline color
         alpha=0.8)
   '''
   #plt.plot(y1,label='Well')
   #plt.plot(y2,label='Recovered')
   plt.legend(loc='upper left')
	

ani = FuncAnimation(plt.gcf(), animate, interval = 0)
plt.show()