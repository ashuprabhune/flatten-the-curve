import matplotlib.pyplot as plt
import pandas as pd
from matplotlib.animation import FuncAnimation

y1 = []
y2 = []

def animate(i):
   data = pd.read_csv('data.csv')
   y1 = data['well']
   y2 = data['recovered']
   plt.cla()
   axes = plt.gca()
   axes.set_xlim(0, 1000)
   axes.set_ylim(0, 50)
   plt.gca().set_facecolor('xkcd:red')
   plt.gca().invert_yaxis()
   row = data.index.values
   col = data.to_numpy()
   plt.fill_between(row, col[:,0], 0,
         facecolor="orange", # The fill color
         color='white',       # The outline color
         alpha=0.8)         # Transparency of the fill
    
   plt.fill_between(row, col[:,1],0,
         facecolor="green", # The fill color
         color='green',     # The outline color
         alpha=0.8) 
   plt.plot(y1,label='Well')
   plt.plot(y2,label='Recovered')
   plt.legend(loc='upper left')
	
	
ani = FuncAnimation(plt.gcf(), animate, interval = 500)
plt.gca().invert_yaxis()
plt.show()