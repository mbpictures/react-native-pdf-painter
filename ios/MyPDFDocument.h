#import <PDFKit/PDFKit.h>
#import "MyPDFPage.h"

@interface MyPDFDocument : PDFDocument

- (void)loadDrawingsFromDisk:(NSString *)filePath;
- (void)saveDrawingsToDisk:(NSString *)filePath;

@end

