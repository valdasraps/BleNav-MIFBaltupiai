import numpy as np

class Normalizer:
    
    def __init__(self, pos_orig, rssi_orig):
        
        self.pos_min = np.min(pos_orig, axis = 0)
        self.pos_max = np.max(pos_orig, axis = 0)
        self.pos_len = self.pos_max - self.pos_min

        self.rssi_min = np.min(rssi_orig)
        self.rssi_max = np.max(rssi_orig)
        self.rssi_len = self.rssi_max - self.rssi_min

    def pos_norm(self, pos_orig):
        return (pos_orig - self.pos_min) / self.pos_len
    
    def rssi_norm(self, rssi_orig):
        return (rssi_orig - self.rssi_min) / self.rssi_len
    
    def pos_orig(self, pos_norm):
        return pos_norm * self.pos_len + self.pos_min
    
    def rssi_orig(self, rssi_norm):
        return rssi_norm * self.rssi_len + self.rssi_min