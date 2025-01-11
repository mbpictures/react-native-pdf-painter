#import "PencilKitCoordinator.h"
#import <PDFKit/PDFKit.h>
#import <PencilKit/PencilKit.h>


@interface PencilKitCoordinator () <PDFPageOverlayViewProvider>


@end

@implementation PencilKitCoordinator

- (instancetype)init {
    self = [super init];
    if (self) {
        _pageToViewMapping = [NSMutableDictionary dictionary];
    }
    return self;
}

- (UIView *)pdfView:(PDFView *)view overlayViewForPage:(PDFPage *)page {
    PKCanvasView *resultView = nil;
    
    // Check if we already have an overlay for the page
    if (self.pageToViewMapping[page.label]) {
        resultView = self.pageToViewMapping[page.label];
    } else {
        // Create a new PKCanvasView if there is no overlay for this page
        PKCanvasView *canvasView = [[PKCanvasView alloc] initWithFrame:CGRectZero];
        canvasView.drawingPolicy = PKCanvasViewDrawingPolicyAnyInput;
        canvasView.overrideUserInterfaceStyle = UIUserInterfaceStyleLight;
        
        canvasView.backgroundColor = [UIColor clearColor];
        self.pageToViewMapping[page.label] = canvasView;
        resultView = canvasView;
        [canvasView becomeFirstResponder];
    }
    
    // If there is an existing drawing, apply it to the canvas
    MyPDFPage *myPDFPage = (MyPDFPage *)page;
    if (myPDFPage.drawing) {
        resultView.drawing = myPDFPage.drawing;
    }
    
    return resultView;
}

- (void)pdfView:(PDFView *)pdfView willEndDisplayingOverlayView:(UIView *)overlayView forPage:(PDFPage *)page {
    PKCanvasView *canvasView = (PKCanvasView *)overlayView;
    MyPDFPage *myPDFPage = (MyPDFPage *)page;
    
    // Save the drawing back to the page before it is discarded
    myPDFPage.drawing = canvasView.drawing;
    
    // Remove the reference to this page's overlay
    [self.pageToViewMapping removeObjectForKey:page.label];
}

-(void)prepareForPersistance:(MyPDFDocument *)document {
    for (NSUInteger pageIndex = 0; pageIndex < document.pageCount; pageIndex++) {
        MyPDFPage *page = (MyPDFPage *)[document pageAtIndex:pageIndex];
        if (self.pageToViewMapping[page.label]) {
            page.drawing = self.pageToViewMapping[page.label].drawing;
        }
    }
}

-(void)undo:(PDFPage *)pdfPage {
    PKCanvasView *canvasView = self.pageToViewMapping[pdfPage.label];
    [[canvasView undoManager] undo];
}

-(void)redo:(PDFPage *)pdfPage {
    PKCanvasView *canvasView = self.pageToViewMapping[pdfPage.label];
    [[canvasView undoManager] redo];
}

-(void)clear:(PDFPage *)pdfPage {
    PKCanvasView *canvasView = self.pageToViewMapping[pdfPage.label];
    [canvasView setDrawing:[[PKDrawing alloc] init]];
}

- (void)setDrawingTool:(PDFPage *)pdfPage brushSettings:(PdfAnnotationViewBrushSettingsStruct)config {
    PKTool *inkingTool;
    NSString * colorString = [[NSString alloc] initWithUTF8String: config.color.c_str()];
    UIColor *toolColor = [self colorFromString:colorString];
    
    switch (config.type) {
        case PdfAnnotationViewType::Marker:
            inkingTool = [[PKInkingTool alloc] initWithInkType:PKInkTypePen color:toolColor width:config.size];
            break;
        case PdfAnnotationViewType::PressurePen:
            inkingTool = [[PKInkingTool alloc] initWithInkType:PKInkTypePencil color:toolColor width:config.size];
            break;
        case PdfAnnotationViewType::Highlighter:
            inkingTool = [[PKInkingTool alloc] initWithInkType:PKInkTypeMarker color:toolColor width:config.size];
            break;
        case PdfAnnotationViewType::Eraser:
            if (@available(iOS 16.4, *)) {
                inkingTool = [[PKEraserTool alloc] initWithEraserType:PKEraserTypeVector width:config.size];
            } else {
                inkingTool = [[PKEraserTool alloc] initWithEraserType:PKEraserTypeVector];
            }
            break;
    }
    
    // Set the tool to the canvas
    PKCanvasView *canvasView = self.pageToViewMapping[pdfPage.label];
    canvasView.tool = inkingTool;
}

- (void)setToolPickerVisible:(PDFPage *)pdfPage isVisible:(bool)visible {
    MyPDFKitToolPickerModel *model = [MyPDFKitToolPickerModel sharedInstance];
    PKCanvasView *canvasView = self.pageToViewMapping[pdfPage.label];
    [model.toolPicker setVisible:visible forFirstResponder:canvasView];
    if (visible) {
        [model.toolPicker addObserver:canvasView];
    } else {
        [model.toolPicker removeObserver:canvasView];
    }
    [canvasView becomeFirstResponder];
}

// Helper method to convert a color string (hex with optional alpha) to UIColor
- (UIColor *)colorFromString:(NSString *)colorString {
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

