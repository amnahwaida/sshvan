import struct
import sys

def parse_class(filename):
    with open(filename, 'rb') as f:
        magic, minor, major, pool_count = struct.unpack('>IHHH', f.read(10))
        pool = [None] * pool_count
        i = 1
        while i < pool_count:
            tag = struct.unpack('>B', f.read(1))[0]
            if tag == 1: # Utf8
                length = struct.unpack('>H', f.read(2))[0]
                pool[i] = f.read(length).decode('utf-8', 'ignore')
            elif tag in (3, 4, 9, 10, 11, 12):
                f.read(4)
            elif tag in (5, 6):
                f.read(8)
                i += 1
            elif tag in (7, 8, 16, 19, 20):
                f.read(2)
            elif tag in (15,):
                f.read(3)
            elif tag in (18,):
                f.read(4)
            i += 1
        
        for item in pool:
            if item:
                if '(' in item or 'set' in item or 'get' in item or 'connect' in item:
                    print(item)

parse_class(sys.argv[1])
