#define PASS_CODE 0xc0ffee
#define FAIL_CODE 0xdeaddead

void halt(int code);

__attribute__((noinline))
void check(int cond) {
  if (!cond) halt(FAIL_CODE);
}

void bubble_sort(int arr[], int n)
{
    int i, j;
    for (i = 0; i < n - 1; i++)     
        for (j = 0; j < n - i - 1; j++) 
            if (arr[j] > arr[j + 1])
                swap(&arr[j], &arr[j + 1]);
}
void swap(int *a, int *b)
{
    int temp = *a;
    *a = *b;
    *b = temp;
}

int main() {
    int arr[5] = { 5, 1, 4, 2, 8 };
    bubble_sort(arr, 5);
    
    check(arr[0] == 1);
    check(arr[1] == 2);
    check(arr[2] == 4);
    check(arr[3] == 5);
    check(arr[4] == 8);

    halt(PASS_CODE);
}