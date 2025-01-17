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
      [_view usePageViewController:true withViewOptions:NULL];

      [self updateThumbnailMode:false];
  }

  return self;
}

- (void)handleTap:(UITapGestureRecognizer *)sender {
    CGPoint touchLocation = [sender locationInView:_view];
    CGFloat screenWidth = _view.bounds.size.width;

    NSInteger delta = 0;
    if (touchLocation.x < screenWidth * 0.25) {
        delta = -1;
    } else if (touchLocation.x > screenWidth * 0.75) {
        delta = 1;
    }
    
    PDFPage *currentPage = _view.currentPage;
    if (currentPage) {
        NSUInteger currentIndex = [currentPage.document indexForPage:currentPage];
        NSUInteger nextIndex = currentIndex + delta;
        if (nextIndex >= 0 && nextIndex < _view.document.pageCount) {
            [_view goToPage:[currentPage.document pageAtIndex:nextIndex]];
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
        PdfAnnotationViewEventEmitter::OnPageCount result = PdfAnnotationViewEventEmitter::OnPageCount{(int)_view.document.pageCount};
        if (_eventEmitter != nullptr) {
           std::dynamic_pointer_cast<const PdfAnnotationViewEventEmitter>(_eventEmitter)
            ->onPageCount(result);
         }
        
        _view.minScaleFactor = _view.scaleFactorForSizeToFit;
        _view.maxScaleFactor = 4.0;
        _view.scaleFactor = _view.scaleFactorForSizeToFit;
    }
    if (oldViewProps.brushSettings.size != newViewProps.brushSettings.size || oldViewProps.brushSettings.type != newViewProps.brushSettings.type || oldViewProps.brushSettings.color != newViewProps.brushSettings.color) {
        if (@available(iOS 16.0, *)) {
            [_view setInMarkupMode:newViewProps.brushSettings.type != PdfAnnotationViewType::None];
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
        [_view goToPage:[_view.document pageAtIndex:(NSUInteger) args[0]]];
    }
}

Class<RCTComponentViewProtocol> PdfAnnotationViewCls(void)
{
    return PdfAnnotationView.class;
}

- hexStringToColor:(NSString *)stringToConvert
{
    NSString *noHashString = [stringToConvert stringByReplacingOccurrencesOfString:@"#" withString:@""];
    NSScanner *stringScanner = [NSScanner scannerWithString:noHashString];

    unsigned hex;
    if (![stringScanner scanHexInt:&hex]) return nil;
    int r = (hex >> 16) & 0xFF;
    int g = (hex >> 8) & 0xFF;
    int b = (hex) & 0xFF;

    return [UIColor colorWithRed:r / 255.0f green:g / 255.0f blue:b / 255.0f alpha:1.0f];
}

@end
