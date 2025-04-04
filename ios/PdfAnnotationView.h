#import <React/RCTViewComponentView.h>
#import <UIKit/UIKit.h>
#import <PDFKit/PDFKit.h>
#import "PencilKitCoordinator.h"
#import "MyPDFDocument.h"
#import "CustomPdfView.h"

#ifndef PdfAnnotationViewNativeComponent_h
#define PdfAnnotationViewNativeComponent_h

NS_ASSUME_NONNULL_BEGIN

@interface PdfAnnotationView : RCTViewComponentView<PencilKitCoordinatorDelegate>
@end

NS_ASSUME_NONNULL_END

#endif /* PdfAnnotationViewNativeComponent_h */
