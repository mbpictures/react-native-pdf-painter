#import <PDFKit/PDFKit.h>
#import <PencilKit/PencilKit.h>

NS_ASSUME_NONNULL_BEGIN

// MyPDFPage is a subclass of PDFPage
@interface MyPDFPage : PDFPage

@property (nonatomic, strong, nullable) PKDrawing *drawing; // Custom drawing property

@end

NS_ASSUME_NONNULL_END

