
#import <Foundation/Foundation.h>
#import <PencilKit/PencilKit.h>
#import <UIKit/UIKit.h>

@interface MyPDFKitToolPickerModel : NSObject

@property (nonatomic, strong) PKToolPicker *toolPicker;

+ (instancetype)sharedInstance;

@end
