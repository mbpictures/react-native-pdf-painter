#import "PdfAnnotationView.h"

#import "generated/RNPdfAnnotationViewSpec/ComponentDescriptors.h"
#import "generated/RNPdfAnnotationViewSpec/EventEmitters.h"
#import "generated/RNPdfAnnotationViewSpec/Props.h"
#import "generated/RNPdfAnnotationViewSpec/RCTComponentViewHelpers.h"

#import "RCTFabricComponentsPlugins.h"

using namespace facebook::react;

@interface PdfAnnotationView () <RCTPdfAnnotationViewViewProtocol>

@end

@implementation PdfAnnotationView {
    PDFView * _view;
    PencilKitCoordinator * _pencilKitCoordinator;
    RoundedTriangleAnnotation *firstLinkAnnotation;
    NSUInteger firstLinkPageIndex;
}

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
    return concreteComponentDescriptorProvider<PdfAnnotationViewComponentDescriptor>();
}

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const PdfAnnotationViewProps>();
    _props = defaultProps;

      _view = [[PDFView alloc] initWithFrame:frame];
      _view.displayMode = kPDFDisplaySinglePage;
      _view.displayDirection = kPDFDisplayDirectionHorizontal;
      _view.autoScales = true;
      _pencilKitCoordinator = [[PencilKitCoordinator alloc] init];
      _pencilKitCoordinator.delegate = self;
      if (@available(iOS 16.0, *)) {
          _view.pageOverlayViewProvider = _pencilKitCoordinator;
      }
      
      [[NSNotificationCenter defaultCenter] addObserver:self
                                               selector:@selector(handlePageChange:)
                                               name:PDFViewPageChangedNotification
                                               object:_view];

      UITapGestureRecognizer *tapRecognizer = [[UITapGestureRecognizer alloc] initWithTarget:self action:@selector(handleTap:)];
      tapRecognizer.numberOfTapsRequired = 1;
      tapRecognizer.cancelsTouchesInView = NO;
      [_view addGestureRecognizer:tapRecognizer];
      
      UILongPressGestureRecognizer *longPressRecognizer = [[UILongPressGestureRecognizer alloc] initWithTarget:self action:@selector(handleLongPress:)];
      longPressRecognizer.minimumPressDuration = 0.5; // Mindestens 0,5 Sek. gedrückt halten
      [_view addGestureRecognizer:longPressRecognizer];
      
      [_view usePageViewController:true withViewOptions:NULL];
      [_view setEnableDataDetectors:YES];
      

      [self updateThumbnailMode:false];
  }

  return self;
}

- (void)handleLongPress:(UILongPressGestureRecognizer *)gestureRecognizer {
    if (gestureRecognizer.state != UIGestureRecognizerStateBegan) return;

    CGPoint locationInView = [gestureRecognizer locationInView:_view];
    PDFPage *currentPage = _view.currentPage;

    if (!currentPage) return;

    CGPoint locationOnPage = [_view convertPoint:locationInView toPage:currentPage];
    const auto &props = *std::static_pointer_cast<PdfAnnotationViewProps const>(_props);

    for (PDFAnnotation *annotation in currentPage.annotations) {
        if (CGRectContainsPoint(annotation.bounds, locationOnPage)) {
            [currentPage removeAnnotation:annotation];
            
            if (props.autoSave) {
                NSString * filePath = [[NSString alloc] initWithUTF8String: props.annotationFile.c_str()];
                [_pencilKitCoordinator prepareForPersistance:(MyPDFDocument *)_view.document];
                [(MyPDFDocument* )_view.document saveDrawingsToDisk:filePath];
            }
            break;
        }
    }
}

- (void)handleTap:(UITapGestureRecognizer *)sender {
    CGPoint touchLocation = [sender locationInView:_view];
    CGFloat screenWidth = _view.bounds.size.width;
    const auto &props = *std::static_pointer_cast<PdfAnnotationViewProps const>(_props);
    bool addLink = props.brushSettings.type == PdfAnnotationViewType::Link;

    PDFPage *currentPage = _view.currentPage;
    CGPoint convertedPoint = [_view convertPoint:touchLocation toPage:currentPage];
    
    if (addLink) {
        NSUInteger currentPageIndex = [_view.document indexForPage:currentPage];
        Float size = props.brushSettings.size;
        RoundedTriangleAnnotation *linkAnnotation = [[RoundedTriangleAnnotation alloc] initWithBounds:CGRectMake(convertedPoint.x - size / 2, convertedPoint.y - size / 2, size, size) forType:PDFAnnotationSubtypeWidget withProperties:nil];
        linkAnnotation.backgroundColor = [self hexStringToColor:[NSString stringWithUTF8String:props.brushSettings.color.c_str()]];
        

        [currentPage addAnnotation:linkAnnotation];
        
        
        if (!firstLinkAnnotation) {
            firstLinkAnnotation = linkAnnotation;
            firstLinkPageIndex = currentPageIndex;
        } else {
            PDFDestination *dest1 = [[PDFDestination alloc] initWithPage:[_view.document pageAtIndex:currentPageIndex] atPoint:CGPointZero];
            PDFDestination *dest2 = [[PDFDestination alloc] initWithPage:[_view.document pageAtIndex:firstLinkPageIndex] atPoint:CGPointZero];
            
            firstLinkAnnotation.rotation = firstLinkPageIndex > currentPageIndex ? 0 : 180;
            linkAnnotation.rotation = firstLinkPageIndex > currentPageIndex ? 180 : 0;
            
            firstLinkAnnotation.action = [[PDFActionGoTo alloc] initWithDestination:dest1];
            linkAnnotation.action = [[PDFActionGoTo alloc] initWithDestination:dest2];
            linkAnnotation.backgroundColor = [linkAnnotation.backgroundColor colorWithAlphaComponent:CGColorGetAlpha(linkAnnotation.backgroundColor.CGColor) * 0.5];
            
            firstLinkAnnotation = nil;
            
            PdfAnnotationViewEventEmitter::OnLinkCompleted event = PdfAnnotationViewEventEmitter::OnLinkCompleted{static_cast<int>(firstLinkPageIndex), static_cast<int>(currentPageIndex)};
            if (_eventEmitter != nullptr) {
               std::dynamic_pointer_cast<const PdfAnnotationViewEventEmitter>(_eventEmitter)
                ->onLinkCompleted(event);
            }
            
            if (props.autoSave) {
                NSString * filePath = [[NSString alloc] initWithUTF8String: props.annotationFile.c_str()];
                [_pencilKitCoordinator prepareForPersistance:(MyPDFDocument *)_view.document];
                [(MyPDFDocument* )_view.document saveDrawingsToDisk:filePath];
            }
        }
        [_view layoutDocumentView];
        return;
    }
    
    NSArray<PDFAnnotation *> *annotations = [currentPage annotations];
    for (PDFAnnotation *annotation in annotations) {
        if (CGRectContainsPoint(annotation.bounds, convertedPoint)) {
            if ([annotation isKindOfClass:[PDFAnnotation class]] && annotation.action) {
                if ([annotation.action isKindOfClass:[PDFActionGoTo class]]) {
                    PDFActionGoTo *goToAction = (PDFActionGoTo *)annotation.action;
                    [_view performSelector:@selector(goToDestination:) withObject:goToAction.destination afterDelay:0.1];
                    return;
                }
            }
        }
    }

    NSInteger delta = 0;
    if (touchLocation.x < screenWidth * 0.25 && !addLink) {
        delta = -1;
    } else if (touchLocation.x > screenWidth * 0.75 && !addLink) {
        delta = 1;
    } else {
        PdfAnnotationViewEventEmitter::OnTap event = PdfAnnotationViewEventEmitter::OnTap{touchLocation.x, touchLocation.y};
        if (_eventEmitter != nullptr) {
           std::dynamic_pointer_cast<const PdfAnnotationViewEventEmitter>(_eventEmitter)
            ->onTap(event);
        }
    }

    if (currentPage) {
        NSUInteger currentIndex = [currentPage.document indexForPage:currentPage];
        NSUInteger nextIndex = currentIndex + delta;
        if (nextIndex >= 0 && nextIndex < _view.document.pageCount) {
            [_view goToPage:[currentPage.document pageAtIndex:nextIndex]];
        } else {
            PdfAnnotationViewEventEmitter::OnDocumentFinished event = PdfAnnotationViewEventEmitter::OnDocumentFinished{delta > 0};
            if (_eventEmitter != nullptr) {
               std::dynamic_pointer_cast<const PdfAnnotationViewEventEmitter>(_eventEmitter)
                ->onDocumentFinished(event);
            }
        }
    }
}

- (void)handlePageChange:(NSNotification *)notification {
    NSUInteger index = [_view.document indexForPage:_view.currentPage];
    PdfAnnotationViewEventEmitter::OnPageChange event = PdfAnnotationViewEventEmitter::OnPageChange{(int)index};
    if (_eventEmitter != nullptr) {
       std::dynamic_pointer_cast<const PdfAnnotationViewEventEmitter>(_eventEmitter)
        ->onPageChange(event);
     }
}

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps
{
    const auto &oldViewProps = *std::static_pointer_cast<PdfAnnotationViewProps const>(_props);
    const auto &newViewProps = *std::static_pointer_cast<PdfAnnotationViewProps const>(props);

    if (oldViewProps.pdfUrl != newViewProps.pdfUrl) {
        NSString * pdfUrl = [[NSString alloc] initWithUTF8String: newViewProps.pdfUrl.c_str()];
        if ([pdfUrl hasPrefix:@"file://"]) {
            pdfUrl = [pdfUrl stringByReplacingOccurrencesOfString:@"file://" withString:@""];
        }
        pdfUrl = [pdfUrl stringByRemovingPercentEncoding];
        NSURL* url = [NSURL fileURLWithPath:pdfUrl isDirectory:NO];
        _view.document = [[MyPDFDocument alloc] initWithURL:url];
        dispatch_async(dispatch_get_main_queue(), ^{
            PdfAnnotationViewEventEmitter::OnPageCount result = PdfAnnotationViewEventEmitter::OnPageCount{(int)self->_view.document.pageCount};
            if (self->_eventEmitter != nullptr) {
                std::dynamic_pointer_cast<const PdfAnnotationViewEventEmitter>(self->_eventEmitter)
                ->onPageCount(result);
             }
        });
        
        _view.minScaleFactor = _view.scaleFactorForSizeToFit;
        _view.maxScaleFactor = 4.0;
        _view.scaleFactor = _view.scaleFactorForSizeToFit;
    }
    if ((oldViewProps.brushSettings.size != newViewProps.brushSettings.size || oldViewProps.brushSettings.type != newViewProps.brushSettings.type || oldViewProps.brushSettings.color != newViewProps.brushSettings.color)) {
        if (@available(iOS 16.0, *)) {
            [_view setInMarkupMode:newViewProps.brushSettings.type != PdfAnnotationViewType::None && newViewProps.brushSettings.type != PdfAnnotationViewType::Link];
        }
        [_pencilKitCoordinator setDrawingTool:_view.currentPage brushSettings:newViewProps.brushSettings];
    }
    if (oldViewProps.iosToolPickerVisible != newViewProps.iosToolPickerVisible) {
        if (@available(iOS 16.0, *)) {
            [_view setInMarkupMode:newViewProps.iosToolPickerVisible];
        }
        [_pencilKitCoordinator setToolPickerVisible:_view.currentPage isVisible:newViewProps.iosToolPickerVisible];
    }
    if (oldViewProps.annotationFile != newViewProps.annotationFile) {
        NSString * filePath = [[NSString alloc] initWithUTF8String: newViewProps.annotationFile.c_str()];
        [(MyPDFDocument* )_view.document loadDrawingsFromDisk:filePath];
        [_pencilKitCoordinator updateDrawings:(MyPDFDocument *)_view.document];
    }
    if (oldViewProps.thumbnailMode != newViewProps.thumbnailMode) {
        [self updateThumbnailMode:newViewProps.thumbnailMode];
    }

    [super updateProps:props oldProps:oldProps];
}

- (void)pencilKitCoordinatorDrawingDidChange:(PencilKitCoordinator *)coordinator {
    const auto &props = *std::static_pointer_cast<PdfAnnotationViewProps const>(_props);
    if (!props.autoSave) {
        return;
    }
    NSString * filePath = [[NSString alloc] initWithUTF8String: props.annotationFile.c_str()];
    [_pencilKitCoordinator prepareForPersistance:(MyPDFDocument *)_view.document];
    [(MyPDFDocument* )_view.document saveDrawingsToDisk:filePath];
}

- (void)updateThumbnailMode:(bool) isThumbnail {
    if (isThumbnail) {
        dispatch_async(dispatch_get_main_queue(), ^{
            PDFDocument *document = self->_view.document;
            if (document) {
                PDFPage *firstPage = [document pageAtIndex:0];
                UIImage *thumbnailImage = [firstPage thumbnailOfSize:self->_view.frame.size forBox:kPDFDisplayBoxMediaBox];
                UIImageView *imageView = [[UIImageView alloc] initWithImage:thumbnailImage];
                imageView.frame = self->_view.frame;
                self.contentView = imageView;
            }
        });
    } else {
        self.contentView = _view;
    }
}

- (void)handleCommand:(const NSString *)commandName args:(const NSArray *)args {
    if ([commandName isEqual:@"saveAnnotations"]) {
        if (args.count == 0) {
            return NSLog(@"Missing parameter for loading annotations!");
        }
        [_pencilKitCoordinator prepareForPersistance:(MyPDFDocument *)_view.document];
        [(MyPDFDocument* )_view.document saveDrawingsToDisk:(NSString*) args[0]];
    }
    if ([commandName isEqual:@"loadAnnotations"]) {
        if (args.count == 0) {
            return NSLog(@"Missing parameter for loading annotations!");
        }
        [(MyPDFDocument* )_view.document loadDrawingsFromDisk:(NSString*) args[0]];
        [_pencilKitCoordinator updateDrawings:(MyPDFDocument *)_view.document];
    }
    if ([commandName isEqual:@"undo"]) {
        [_pencilKitCoordinator undo:_view.currentPage];
    }
    if ([commandName isEqual:@"redo"]) {
        [_pencilKitCoordinator redo:_view.currentPage];
    }
    if ([commandName isEqual:@"clear"]) {
        [_pencilKitCoordinator clear:_view.currentPage];
    }
    if ([commandName isEqual:@"setPage"]) {
        if (args.count == 0) {
            return NSLog(@"Missing parameter page!");
        }
        id firstElement = [args objectAtIndex:0];
        if ([firstElement respondsToSelector:@selector(integerValue)]) {
            NSInteger firstInt = [firstElement integerValue];
            [_view goToPage:[_view.document pageAtIndex:firstInt]];
        }
    }
}

Class<RCTComponentViewProtocol> PdfAnnotationViewCls(void)
{
    return PdfAnnotationView.class;
}

- (UIColor *)hexStringToColor:(NSString *)colorString {
    // Ensure the string starts with "#" and has the correct length (8 characters for AARRGGBB format)
    if ([colorString hasPrefix:@"#"] && (colorString.length == 9 || colorString.length == 7)) {
        // Remove the "#" and process the hex string
        NSString *hexString = [colorString substringFromIndex:1];
        unsigned int hexValue;
        NSScanner *scanner = [NSScanner scannerWithString:hexString];
        [scanner scanHexInt:&hexValue];
        
        // Extract alpha, red, green, and blue components
        CGFloat alpha = ((hexValue >> 24) & 0xFF) / 255.0;
        CGFloat red = ((hexValue >> 16) & 0xFF) / 255.0;
        CGFloat green = ((hexValue >> 8) & 0xFF) / 255.0;
        CGFloat blue = (hexValue & 0xFF) / 255.0;
        
        if (hexString.length == 8) {
            return [UIColor colorWithRed:red green:green blue:blue alpha:alpha];
        }
        if (hexString.length == 6) {
            return [UIColor colorWithRed:red green:green blue:blue alpha:1];
        }
    }
    
    // If the string is not a valid hex color with alpha or RGB, return default color (black)
    return [UIColor blackColor];
}

@end
