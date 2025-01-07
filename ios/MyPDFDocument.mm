
#import "MyPDFDocument.h"

@implementation MyPDFDocument

// Override the pageClass getter to return MyPDFPage
- (Class)pageClass {
    return [MyPDFPage class];
}

- (void)saveDrawingsToDisk:(NSString *)filePath {
    if ([filePath hasPrefix:@"file://"]) {
        filePath = [filePath stringByReplacingOccurrencesOfString:@"file://" withString:@""];
    }
    
    NSMutableDictionary *drawingsData = [NSMutableDictionary dictionary];
    
    for (NSUInteger pageIndex = 0; pageIndex < self.pageCount; pageIndex++) {
        MyPDFPage *page = (MyPDFPage *)[self pageAtIndex:pageIndex];
        if (page.drawing) {
            // Archive the drawing using secure coding
            NSData *drawingData = [NSKeyedArchiver archivedDataWithRootObject:page.drawing requiringSecureCoding:NO error:NULL];
            [drawingsData setObject:drawingData forKey:@(pageIndex)];
        }
    }
    
    // Archive the dictionary of drawings using secure coding
    NSData *data = [NSKeyedArchiver archivedDataWithRootObject:drawingsData requiringSecureCoding:NO error:NULL];
    [data writeToFile:filePath atomically:YES];
}


- (void)loadDrawingsFromDisk:(NSString *)filePath {
    if ([filePath hasPrefix:@"file://"]) {
        filePath = [filePath stringByReplacingOccurrencesOfString:@"file://" withString:@""];
    }

    NSData *data = [NSData dataWithContentsOfFile:filePath];
    NSError *error = nil;
    NSKeyedUnarchiver *unarchiver = [[NSKeyedUnarchiver alloc] initForReadingFromData:data error:&error];
    if (error) {
        NSLog(@"Error initializing unarchiver: %@", error);
        return;
    }
    unarchiver.requiresSecureCoding = NO;
    NSDictionary *drawingsData = [unarchiver decodeObjectOfClass:[NSDictionary class] forKey:NSKeyedArchiveRootObjectKey];
    
    if (error) {
        NSLog(@"Error decoding object: %@", error);
        return;
    }
    
    if (drawingsData) {
        for (NSNumber *pageIndexKey in drawingsData) {
            NSData *drawingData = drawingsData[pageIndexKey];
            
            PKDrawing *drawing = [NSKeyedUnarchiver unarchivedObjectOfClass:[PKDrawing class] fromData:drawingData error:&error];
            if (error) {
                NSLog(@"Error unarchiving drawing: %@", error);
                continue;
            }
            if (drawing) {
                MyPDFPage *page = (MyPDFPage *)[self pageAtIndex:pageIndexKey.integerValue];
                page.drawing = drawing;
            }
        }
    }

    // Finish reading from the unarchiver
    [unarchiver finishDecoding];
}



@end
