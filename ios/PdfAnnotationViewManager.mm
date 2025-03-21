#import <React/RCTViewManager.h>
#import <React/RCTUIManager.h>
#import "RCTBridge.h"

@interface PdfAnnotationViewManager : RCTViewManager
@end

@implementation PdfAnnotationViewManager

RCT_EXPORT_MODULE(PdfAnnotationView)

- (UIView *)view
{
  return [[UIView alloc] init];
}

RCT_EXPORT_VIEW_PROPERTY(color, NSString)

@end
