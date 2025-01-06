
#import "MyPDFKitToolPickerModel.h"

@implementation MyPDFKitToolPickerModel

// Singleton pattern
+ (instancetype)sharedInstance {
    static MyPDFKitToolPickerModel *sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedInstance = [[MyPDFKitToolPickerModel alloc] init];
    });
    return sharedInstance;
}

- (instancetype)init {
    self = [super init];
    NSLog(@"Init Toolpicker");
    if (self) {
        _toolPicker = [[PKToolPicker alloc] init];
        NSLog(@"Init Toolpicker initialized");
    }
    return self;
}

@end
