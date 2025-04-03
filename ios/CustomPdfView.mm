#import "CustomPdfView.h"

@implementation CustomPdfView

- (BOOL)canPerformAction:(SEL)action withSender:(id)sender {
    return NO;
}

- (void)addGestureRecognizer:(UIGestureRecognizer *)gestureRecognizer {
    // disable double tap for better navigation speed
    if ([gestureRecognizer isKindOfClass:[UITapGestureRecognizer class]]) {
        UITapGestureRecognizer *tapGesture = (UITapGestureRecognizer *)gestureRecognizer;
        if (tapGesture.numberOfTapsRequired == 2) {
            gestureRecognizer.enabled = NO;
        }
    }

    [super addGestureRecognizer:gestureRecognizer];
}

@end
