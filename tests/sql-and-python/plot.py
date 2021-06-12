import numpy as np
import matplotlib.pyplot as plt
import sys

if len(sys.argv) != 3:
    sys.exit('Wrong number of arguments: ' + str(len(sys.argv) - 1))

file_path = sys.argv[1]
plot_path = sys.argv[2]

data = np.genfromtxt(file_path, delimiter=',', skip_header=1, dtype=np.dtype(object))
data = np.atleast_2d(data)
params = np.vstack({tuple(row) for row in data[:,0:2]})

naive = b'naive'

for param in params:
    sub_data = data[(data[:,0] == param[0]) & (data[:,1] == param[1])]

    plot_label = ""
    if param[0] == naive:
        plot_label = plot_label + 'naive'
    else:
        plot_label = plot_label + str(int(param[0])) + ' %'
    plot_label = plot_label + '  -  '
    if param[1] == naive:
        plot_label = plot_label + 'naive'
    else:
        plot_label = plot_label + str(int(param[1]))

    x = sub_data[:,2].astype('int32')
    y = sub_data[:,3].astype('float64')
    plt.plot(x, y, label=plot_label)

plt.xlabel('Operations planned')
plt.ylabel('Operations executed')
plt.legend()

plt.savefig(plot_path)
