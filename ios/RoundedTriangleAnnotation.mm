#import "RoundedTriangleAnnotation.h"

@implementation RoundedTriangleAnnotation

- (void)drawWithBox:(PDFDisplayBox)box inContext:(CGContextRef)context {
    // Draw original content under the new content.
    [super drawWithBox:box inContext:context];
    
    // Draw a custom rounded triangle.
    UIGraphicsPushContext(context);
    CGContextSaveGState(context);
    
    // Apply rotation
    CGContextTranslateCTM(context, CGRectGetMidX(self.bounds), CGRectGetMidY(self.bounds));
    CGContextRotateCTM(context, (self.rotation - 90) * M_PI / 180.0);
    CGContextTranslateCTM(context, -CGRectGetMidX(self.bounds), -CGRectGetMidY(self.bounds));
    
    // Define the path for the rounded triangle.
    UIBezierPath *path = [UIBezierPath bezierPath];
    CGFloat minX = CGRectGetMinX(self.bounds);
    CGFloat minY = CGRectGetMinY(self.bounds);
    CGFloat maxX = CGRectGetMaxX(self.bounds);
    CGFloat maxY = CGRectGetMaxY(self.bounds);
    CGFloat midX = CGRectGetMidX(self.bounds);
    
    [path moveToPoint:CGPointMake(midX, minY)];
    [path addLineToPoint:CGPointMake(maxX, maxY)];
    [path addLineToPoint:CGPointMake(minX, maxY)];
    [path closePath];
    
    // Set the fill color.
    [self.backgroundColor setFill];
    [path fill];
    
    CGContextRestoreGState(context);
    UIGraphicsPopContext();
}

@end
