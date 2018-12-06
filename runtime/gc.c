#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <stdbool.h>
#include <sys/time.h>

// The Gimple Garbage Collector.


//===============================================================//
// The Java Heap data structure.

/*   
      ----------------------------------------------------
      |                        |                         |
      ----------------------------------------------------
      ^\                      /^
      | \<~~~~~~~ size ~~~~~>/ |
    from                       to
 */
struct JavaHeap {
  int size;         // in bytes, note that this if for semi-heap size
  char *from;       // the "from" space pointer
  char *fromFree;   // the next "free" space in the from space
  char *to;         // the "to" space pointer
  char *toStart;    // "start" address in the "to" space
  char *toNext;     // "next" free space pointer in the to space
};

struct frame_header {
  void *prev;
  int *argument_base_address;
  char *argument_gc_map;
  char *locals_gc_map;
};

struct object_header {
  void *vptr;
  int isObjOrArray;
  unsigned length;
  void *forwarding;
};

const size_t OBJ_HEADER_SIZE =
    sizeof(void *) + sizeof(int) + sizeof(unsigned) + sizeof(void *);

struct vtable_header {
  char *class_map;
};

// The Java heap, which is initialized by the following
// "heap_init" function.
struct JavaHeap heap;

// Lab 4, exercise 10:
// Given the heap size (in bytes), allocate a Java heap
// in the C heap, initialize the relevant fields.
void Tiger_heap_init(int heapSize) {
  // You should write 7 statement here:
  // #1: allocate a chunk of memory of size "heapSize" using "malloc"
  assert(heapSize > 0);
  char *ptr = (char *) malloc(sizeof(char) * heapSize);
  // #2: initialize the "size" field, note that "size" field
  // is for semi-heap, but "heapSize" is for the whole heap.
  heap.size = heapSize / 2;
  // #3: initialize the "from" field (with what value?)
  heap.from = ptr;
  // #4: initialize the "fromFree" field (with what value?)
  heap.fromFree = ptr;
  // #5: initialize the "to" field (with what value?)
  heap.to = heap.from + heap.size;
  // #6: initizlize the "toStart" field with NULL;
  heap.toStart = NULL;
  // #7: initialize the "toNext" field with NULL;
  heap.toNext = NULL;
}

// The "prev" pointer, pointing to the top frame on the GC stack. 
// (see part A of Lab 4)
void *prev = 0;



//===============================================================//
// Object Model And allocation

static void *alloc(size_t size) {

  if (heap.fromFree + size < heap.from + heap.size) {
    void *ret = heap.fromFree;
    heap.fromFree += size;
    return ret;
  }

  return NULL;
}

static void Tiger_gc();

// Lab 4: exercise 11:
// "new" a new object, do necessary initializations, and
// return the pointer (reference).
/*    ----------------
      | vptr      ---|----> (points to the virtual method table)
      |--------------|
      | isObjOrArray | (0: for normal objects)
      |--------------|
      | length       | (this field should be empty for normal objects)
      |--------------|
      | forwarding   | 
      |--------------|\
p---->| v_0          | \      
      |--------------|  s
      | ...          |  i
      |--------------|  z
      | v_{size-1}   | /e
      ----------------/
*/
// Try to allocate an object in the "from" space of the Java
// heap. Read Tiger book chapter 13.3 for details on the
// allocation.
// There are two cases to consider:
//   1. If the "from" space has enough space to hold this object, then
//      allocation succeeds, return the apropriate address (look at
//      the above figure, be careful);
//   2. if there is no enough space left in the "from" space, then
//      you should call the function "Tiger_gc()" to collect garbages.
//      and after the collection, there are still two sub-cases:
//        a: if there is enough space, you can do allocations just as case 1; 
//        b: if there is still no enough space, you can just issue
//           an error message ("OutOfMemory") and exit.
//           (However, a production compiler will try to expand
//           the Java heap.)
void *Tiger_new(void *vtable, int size) {
  // Your code here:
  if (size <= 0) {
    return NULL;
  }
  size_t asize = size + OBJ_HEADER_SIZE;

  void *ptr;
  if ((ptr = alloc(asize)) == NULL) {
    Tiger_gc();
    if ((ptr = alloc(asize)) == NULL) {
      fprintf(stderr, "OutOfMemory\n");
      exit(EXIT_FAILURE);
    }
  }

  void *ret = ptr;
  assert(ptr);
  *((void **) (ptr)) = vtable;
  ptr += sizeof(void *);
  *((int *) ptr) = 0;
  ptr += sizeof(int);
  *((unsigned *) ptr) = 0;
  ptr += sizeof(unsigned);
  *((void **) ptr) = NULL;
  return ret;
}

// "new" an array of size "length", do necessary
// initializations. And each array comes with an
// extra "header" storing the array length and other information.
/*    ----------------
      | vptr         | (this field should be empty for an array)
      |--------------|
      | isObjOrArray | (1: for array)
      |--------------|
      | length       |
      |--------------|
      | forwarding   | 
      |--------------|\
p---->| e_0          | \      
      |--------------|  s
      | ...          |  i
      |--------------|  z
      | e_{length-1} | /e
      ----------------/
*/
// Try to allocate an array object in the "from" space of the Java
// heap. Read Tiger book chapter 13.3 for details on the
// allocation.
// There are two cases to consider:
//   1. If the "from" space has enough space to hold this array object, then
//      allocation succeeds, return the apropriate address (look at
//      the above figure, be careful);
//   2. if there is no enough space left in the "from" space, then
//      you should call the function "Tiger_gc()" to collect garbages.
//      and after the collection, there are still two sub-cases:
//        a: if there is enough space, you can do allocations just as case 1; 
//        b: if there is still no enough space, you can just issue
//           an error message ("OutOfMemory") and exit.
//           (However, a production compiler will try to expand
//           the Java heap.)
void *Tiger_new_array(int length) {
  // Your code here:
  if (length <= 0) {
    return NULL;
  }
  size_t size = OBJ_HEADER_SIZE + (length * sizeof(int));
  void *ptr;
  if ((ptr = alloc(size)) == NULL) {
    Tiger_gc();
    if ((ptr = alloc(size)) == NULL) {
      fprintf(stderr, "OutOfMemory\n");
      exit(EXIT_FAILURE);
    }
  }

  assert(ptr);
  *((void **) (ptr)) = NULL;
  ptr += sizeof(void *);
  *((int *) ptr) = 1;
  ptr += sizeof(int);
  *((unsigned *) ptr) = (unsigned) length;
  ptr += sizeof(unsigned);
  *((void **) ptr) = NULL;
  ptr += sizeof(void *);
  return ptr;
}

//===============================================================//
// The Gimple Garbage Collector

static bool is_pointer_to_heap(
    const char *ptr, const char *heap_start, size_t heap_size) {
  if (!ptr) {
    return false;
  }
  return (ptr >= heap_start && ptr < heap_start + heap_size);
}

static size_t obj_size(const char *p) {
  size_t size = OBJ_HEADER_SIZE;
  char c;
  while ((c = *p)) {
    if (c == '0') {
      size += sizeof(int);
    } else {
      size += sizeof(void *);
    }
    ++p;
  }
  return size;
}

static struct object_header *copy_obj(struct object_header **pobj) {
  struct object_header *obj = *pobj;

  if (!is_pointer_to_heap((const char *) obj->forwarding, heap.to, (size_t) heap.size)) {
    char *class_map = ((struct vtable_header *) (obj->vptr))->class_map;
    size_t size = obj_size(class_map);
    memcpy(heap.toNext, obj, size);
    obj->forwarding = heap.toNext;
    heap.toNext += size;
  }

  return (struct object_header *) obj->forwarding;
}

static void copy_root(struct frame_header *header) {
  char *base = (char *) header->argument_base_address;
  struct object_header **obj;
  char *p, c;

  p = header->argument_gc_map;
  while ((c = *p++) != 0) {
    if (c == '0') {
      base += sizeof(int);
    } else {
      obj = (struct object_header **) base;
      if (*obj) {
        *obj = copy_obj(obj);
      }
      base += sizeof(void *);
    }
  }

  p = header->locals_gc_map;
  base = ((char *) &header->locals_gc_map) + sizeof(char *);
  while ((c = *p++) != 0) {
    if (c == '0') {
      base += sizeof(int);
    } else {
      obj = (struct object_header **) base;
      if (*obj) {
        *obj = copy_obj(obj);
      }
      base += sizeof(void *);
    }
  }
}

// Lab 4, exercise 12:
// A copying collector based-on Cheney's algorithm.
static void Tiger_gc() {
  static int round = 0;
  // Your code here:
  struct frame_header *header = (struct frame_header *) prev;
  struct timeval start, end;

  gettimeofday(&start, NULL);

  heap.toStart = heap.toNext = heap.to;

  while (header) {
    copy_root(header);
    header = (struct frame_header *) header->prev;
  }

  struct object_header *obj;
  struct object_header **child;
  char c;
  const char *class_map;
  char *addr;

  while (heap.toStart != heap.toNext) {
    obj = (struct object_header *) heap.toStart;
    addr = heap.toStart + OBJ_HEADER_SIZE;
    class_map = ((struct vtable_header *) (obj->vptr))->class_map;
    while ((c = *class_map++) != 0) {
      if (c == '0') {
        addr += sizeof(int);
      } else {
        child = (struct object_header **) addr;
        if (*child) {
          *child = copy_obj(child);
        }
        addr += sizeof(void *);
      }
    }
    heap.toStart = addr;
  }

  void *tmp = heap.from;
  heap.from = heap.to;
  heap.fromFree = heap.toNext;
  heap.to = tmp;
  heap.toNext = heap.toStart = NULL;
  memset(heap.to, 0, heap.size);

  gettimeofday(&end, NULL);
  ++round;

  double interval = (double) (end.tv_sec - start.tv_sec) +
                    ((double) (end.tv_usec - start.tv_usec)) / 1e6;
  printf("%d round of GC: %lfs, collected %d bytes\n",
         round, interval, heap.size - ((int) (heap.fromFree - heap.from)));
}

