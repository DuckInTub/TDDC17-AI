import numpy as np
import matplotlib.pyplot as plt

# Corrected data points
time = np.array([222, 26, 4, 2, 1])
accuracy = np.array([0.8127, 0.8263, 0.8121, 0.7235, 0.1356])
batch_sizes = np.array([1, 10, 100, 1000, 60000])

# Create a figure with three subplots
fig, axs = plt.subplots(3, 1, figsize=(10, 18))

# Batch Size vs Time
axs[0].scatter(batch_sizes, time, color='blue', label='Data Points')
axs[0].set_title('Batch Size vs Time')
axs[0].set_xlabel('Batch Size')
axs[0].set_ylabel('Time (s)')
axs[0].set_xscale('log')  # Log scale for better visualization
axs[0].grid()
axs[0].legend()

# Batch Size vs Accuracy
axs[1].scatter(batch_sizes, accuracy, color='green', label='Data Points')
axs[1].set_title('Batch Size vs Accuracy')
axs[1].set_xlabel('Batch Size')
axs[1].set_ylabel('Accuracy')
axs[1].set_xscale('log')  # Log scale for better visualization
axs[1].grid()
axs[1].legend()

# Time vs Accuracy
axs[2].scatter(time, accuracy, color='red', label='Data Points')
axs[2].set_title('Time vs Accuracy')
axs[2].set_xlabel('Time (s)')
axs[2].set_ylabel('Accuracy')
axs[2].grid()
axs[2].legend()

# Adjust layout
plt.tight_layout()
plt.show()
