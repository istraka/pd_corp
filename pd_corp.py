#!/usr/bin/python3

import sys
from multiprocessing import Lock, Process, Value
from ctypes import c_byte
import os
import time

class BlockingConcurrentPdcorpCommunicator():
    """
    Communicator that provides methods for get and put element. 
    Class implements communicating protocol with pd_corp application through stdout and stdin.
    When function get() return None, It is guaranted that another string will not be inserted into queue from pd_corp app and process may end if internal queue is empty(subprocess has not inserted anything).
    Every instance has its own lock.
    Queue reads and write to sys.stdin therefore it may be necessary to open parent's stdin filedescriptor in child process like sys.stdin = os.fdopen(parentStdinFd).
    """
    def __init__(self):
        self.__lock = Lock()
        self.__closed = Value(c_byte,0);
        self.__lock = Lock()

    def get(self):
        with self.__lock:
            sys.stdout.write("\n")
            sys.stdout.flush()
            
            if self.__closed.value == 1:
                element = None
            
            else:
                element = sys.stdin.readline().strip()
                if element == "exit":
                    self.__closed.value = 1
                    element = None
            return element

    def isEmpty(self):
        with self.__lock:
            return self.__closed.value == 1

    """
    Need to be called after processing is done.
    """
    def fileDone(self,inputFile, *outputFiles):
        with self.__lock:
            sys.stdout.write(inputFile+"\t"+" ".join(map(str,outputFiles))+"\n")
            sys.stdout.flush()



def testProcess(id, queue, fd, sleepTime, destPath):
    sys.stdin = os.fdopen(fd)
    while(True):
        f = queue.get()
        time.sleep(float(sleepTime))
        if f is None:
            break
        filename = f[f.rfind(os.sep)+1:]
        with open(destPath+filename+".out","w") as file:
            file.write(filename)
            queue.fileDone(f, destPath+filename+".out")


if __name__ == "__main__":
    if(len(sys.argv) != 3):
        print("Test usage: "+sys.argv[0]+" <waiting time milis> <destination folder ending with />")
    lock = Lock()
    queue = BlockingConcurrentPdcorpCommunicator()
    l=[]
    for num in range(5):
        p = Process(target=testProcess, args=(num, queue, sys.stdin.fileno(), sys.argv[1], sys.argv[2]))
        p.start()
        l.append(p)

    for p in l:
        p.join()


