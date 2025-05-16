#import <Foundation/Foundation.h>
#import <PDFKit/PDFKit.h>
#import <PencilKit/PencilKit.h>
#import "MyPDFPage.h"
#import <react/renderer/components/RNPdfAnnotationViewSpec/Props.h>
#import "MyPDFKitToolPickerModel.h"
#import "MyPDFDocument.h"

NS_ASSUME_NONNULL_BEGIN

using namespace facebook::react;

@class PencilKitCoordinator;
@protocol PencilKitCoordinatorDelegate <NSObject>
- (void)pencilKitCoordinatorDrawingDidChange:(PencilKitCoordinator *)coordinator;
@end

@interface PencilKitCoordinator : NSObject <PDFPageOverlayViewProvider, PKCanvasViewDelegate>
@property (nonatomic, strong) NSMutableDictionary<NSString *, PKCanvasView *> *pageToViewMapping;
@property (nonatomic, weak) id<PencilKitCoordinatorDelegate> delegate;
@property PdfAnnotationViewBrushSettingsStruct currentBrushSettings;


- (UIView *)pdfView:(PDFView *)view overlayViewForPage:(PDFPage *)page;
- (void)pdfView:(PDFView *)pdfView willEndDisplayingOverlayView:(UIView *)overlayView forPage:(PDFPage *)page;
-(void)prepareForPersistance:(MyPDFDocument *)document;
-(void)updateDrawings:(MyPDFDocument *)document;
- (void)setDrawingTool:(PDFPage *)pdfPage brushSettings:(PdfAnnotationViewBrushSettingsStruct)config;
- (void)setToolPickerVisible:(PDFPage *)pdfPage isVisible:(bool)visible;
- (UIColor *)colorFromString:(NSString *)colorInHex;
-(void)undo:(PDFPage *)pdfPage;
-(void)redo:(PDFPage *)pdfPage;
-(void)clear:(PDFPage *)pdfPage;

@end

NS_ASSUME_NONNULL_END

