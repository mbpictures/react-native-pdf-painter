#import <Foundation/Foundation.h>
#import <PDFKit/PDFKit.h>
#import <PencilKit/PencilKit.h>
#import "MyPDFPage.h"
#import "generated/RNPdfAnnotationViewSpec/Props.h"
#import "MyPDFKitToolPickerModel.h"

NS_ASSUME_NONNULL_BEGIN

using namespace facebook::react;
@interface PencilKitCoordinator : NSObject <PDFPageOverlayViewProvider>

@property (nonatomic, strong) NSMutableDictionary<NSString *, PKCanvasView *> *pageToViewMapping;

- (UIView *)pdfView:(PDFView *)view overlayViewForPage:(PDFPage *)page;
- (void)pdfView:(PDFView *)pdfView willEndDisplayingOverlayView:(UIView *)overlayView forPage:(PDFPage *)page;
- (void)setDrawingTool:(PDFPage *)pdfPage brushSettings:(PdfAnnotationViewBrushSettingsStruct)config;
- (void)setToolPickerVisible:(PDFPage *)pdfPage isVisible:(bool)visible;
- (UIColor *)colorFromString:(NSString *)colorInHex;


@end

NS_ASSUME_NONNULL_END

