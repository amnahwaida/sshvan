import zipfile
import re

with zipfile.ZipFile('classes.jar', 'r') as z:
    try:
        data = z.read('com/zerotier/sockets/ZeroTierNative.class')
        strings = [s.decode('utf-8', 'ignore') for s in re.split(b'[^A-Za-z0-9_/.\-()<>]+', data) if len(s) > 3]
        for s in strings:
            if 'ZTS' in s: print(s)
    except:
        pass
