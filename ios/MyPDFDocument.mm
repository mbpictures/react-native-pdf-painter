
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
    
    NSMutableDictionary *dataToSave = [NSMutableDictionary dictionary];
    
    for (NSUInteger pageIndex = 0; pageIndex < self.pageCount; pageIndex++) {
        MyPDFPage *page = (MyPDFPage *)[self pageAtIndex:pageIndex];
        
        NSMutableDictionary *pageData = [NSMutableDictionary dictionary];
        if (page.drawing) {
            // Archive the drawing using secure coding
            NSData *drawingData = [page.drawing dataRepresentation];
            pageData[@"drawing"] = drawingData;
        }
        
        // Speichern der Link-Annotationen
        NSMutableArray *linkAnnotations = [NSMutableArray array];
        for (PDFAnnotation *annotation in page.annotations) {
            PDFActionGoTo *goToAction = (PDFActionGoTo *)annotation.action;
            NSLog(@"Annotation action: %@", goToAction);
            if ([goToAction isKindOfClass:[PDFActionGoTo class]]) {
                NSUInteger targetPageIndex = [self indexForPage:goToAction.destination.page];
                const CGFloat *components = CGColorGetComponents(annotation.color.CGColor);
                NSDictionary *linkData = @{
                    @"bounds": NSStringFromCGRect(annotation.bounds),
                    @"targetPage": @(targetPageIndex),
                    @"color": @[@(components[0]), @(components[1]), @(components[2]), @(CGColorGetAlpha(annotation.color.CGColor))]
                };
                [linkAnnotations addObject:linkData];
            }
        }

        if (linkAnnotations.count > 0) {
            NSLog(@"Adding Link Annotations");
            pageData[@"links"] = linkAnnotations;
        }
        
        if (pageData.count > 0) {
            dataToSave[@(pageIndex)] = pageData;
        }
    }
    
    // Archive the dictionary of drawings using secure coding
    NSData *data = [NSKeyedArchiver archivedDataWithRootObject:dataToSave requiringSecureCoding:NO error:NULL];
    [data writeToFile:filePath atomically:YES];
}


- (void)loadDrawingsFromDisk:(NSString *)filePath {
    if ([filePath hasPrefix:@"file://"]) {
        filePath = [filePath stringByReplacingOccurrencesOfString:@"file://" withString:@""];
    }

    NSData *data = [NSData dataWithContentsOfFile:filePath];
    if (!data) {
        NSLog(@"⚠️ Error while reading file");
        return;
    }

    NSError *error = nil;
    NSSet *allowedClasses = [NSSet setWithObjects:
        [NSDictionary class], [NSArray class], [NSNumber class], [NSString class],
        [NSData class], [UIColor class], nil];
    id loadedObject = [NSKeyedUnarchiver unarchivedObjectOfClasses:allowedClasses fromData:data error:&error];

    if (error) {
        NSLog(@"❌ Error while unarchiving: %@", error);
        return;
    }

    if ([loadedObject isKindOfClass:[NSDictionary class]]) {
        NSDictionary *loadedData = (NSDictionary *)loadedObject;

        for (NSNumber *pageIndexKey in loadedData) {
            id pageData = loadedData[pageIndexKey];
            MyPDFPage *page = (MyPDFPage *)[self pageAtIndex:pageIndexKey.integerValue];

            if ([pageData isKindOfClass:[NSDictionary class]]) {
                NSDictionary *pageDict = (NSDictionary *)pageData;

                if ([pageDict[@"drawing"] isKindOfClass:[NSData class]]) {
                    NSData *drawingData = pageDict[@"drawing"];
                    PKDrawing *drawing = [[PKDrawing alloc] initWithData:drawingData error:&error];
                    if (!error) {
                        page.drawing = drawing;
                    }
                }

                if ([pageDict[@"links"] isKindOfClass:[NSArray class]]) {
                    NSArray *linkAnnotations = pageDict[@"links"];
                    for (NSDictionary *linkData in linkAnnotations) {
                        //if (![linkData isKindOfClass:[NSDictionary class]]) continue;

                        CGRect bounds = CGRectFromString(linkData[@"bounds"]);
                        NSUInteger targetPageIndex = [linkData[@"targetPage"] unsignedIntegerValue];
                        NSArray *colorComponents = linkData[@"color"];
                        UIColor *color = [UIColor colorWithRed:[colorComponents[0] floatValue]
                                                                                             green:[colorComponents[1] floatValue]
                                                                                              blue:[colorComponents[2] floatValue]
                                                                                             alpha:[colorComponents[3] floatValue]];
                        
                        NSLog(@"Addining Annotation with color: %@", color);

                        PDFAnnotation *linkAnnotation = [[PDFAnnotation alloc] initWithBounds:bounds forType:PDFAnnotationSubtypeHighlight withProperties:nil];
                        linkAnnotation.backgroundColor = color;
                        PDFActionGoTo *goToAction = [[PDFActionGoTo alloc] initWithDestination:[[PDFDestination alloc] initWithPage:[self pageAtIndex:targetPageIndex] atPoint:CGPointZero]];
                        linkAnnotation.action = goToAction;

                        [page addAnnotation:linkAnnotation];
                    }
                }
            } else if ([pageData isKindOfClass:[NSData class]]) {
                // backwards compatibility when no links were stored!
                NSData *drawingData = (NSData *)pageData;
                PKDrawing *drawing = [[PKDrawing alloc] initWithData:drawingData error:&error];
                if (!error) {
                    page.drawing = drawing;
                }
            }
        }
    }
}



@end
