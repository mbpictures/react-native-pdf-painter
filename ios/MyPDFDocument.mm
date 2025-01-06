
#import "MyPDFDocument.h"

@implementation MyPDFDocument

// Override the pageClass getter to return MyPDFPage
- (Class)pageClass {
    return [MyPDFPage class];
}

@end
