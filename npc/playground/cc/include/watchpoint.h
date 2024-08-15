#define NR_WP 32

typedef struct watchpoint {
  int NO;
  struct watchpoint *next;

  /* TODO: Add more members if necessary */
  char expr[128];
  uint64_t before_value;

} WP;

static WP wp_pool[NR_WP] __attribute__((used)) = {};
static WP *head __attribute__((used)) = NULL, *free_ __attribute__((used)) = NULL;

WP* new_wp();

int free_wp(WP *wp);
WP* find_wp(int NO);

void watchpoints_display();
uint64_t exprAgain(char *e, bool *success);
int check_wp();