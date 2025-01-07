#import "PdfAnnotationView.h"

#import "generated/RNPdfAnnotationViewSpec/ComponentDescriptors.h"
#import "generated/RNPdfAnnotationViewSpec/EventEmitters.h"
#import "generated/RNPdfAnnotationViewSpec/Props.h"
#import "generated/RNPdfAnnotationViewSpec/RCTComponentViewHelpers.h"

#import "RCTFabricComponentsPlugins.h"

using namespace facebook::react;

@interface PdfAnnotationView () <RCTPdfAnnotationViewViewProtocol>

@end

@implementation PdfAnnotationView {
    PDFView * _view;
    PencilKitCoordinator * _pencilKitCoordinator;
}

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
    return concreteComponentDescriptorProvider<PdfAnnotationViewComponentDescriptor>();
}

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const PdfAnnotationViewProps>();
    _props = defaultProps;

      _view = [[PDFView alloc] initWithFrame:frame];
      _view.displayMode = kPDFDisplaySinglePage;
      _view.displayDirection = kPDFDisplayDirectionHorizontal;
      _view.autoScales = true;
      _pencilKitCoordinator = [[PencilKitCoordinator alloc] init];
      _view.pageOverlayViewProvider = _pencilKitCoordinator;
      [_view usePageViewController:true withViewOptions:NULL];

    self.contentView = _view;
  }

  return self;
}

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps
{
    const auto &oldViewProps = *std::static_pointer_cast<PdfAnnotationViewProps const>(_props);
    const auto &newViewProps = *std::static_pointer_cast<PdfAnnotationViewProps const>(props);

    if (oldViewProps.pdfUrl != newViewProps.pdfUrl) {
        NSString * pdfUrl = [[NSString alloc] initWithUTF8String: newViewProps.pdfUrl.c_str()];
        NSURL* url = [NSURL URLWithString:pdfUrl];
        _view.document = [[MyPDFDocument alloc] initWithURL:url];
    }
    if (oldViewProps.brushSettings.size != newViewProps.brushSettings.size || oldViewProps.brushSettings.type != newViewProps.brushSettings.type || oldViewProps.brushSettings.color != newViewProps.brushSettings.color) {
        [_view setInMarkupMode:newViewProps.brushSettings.type != PdfAnnotationViewType::None];
        [_pencilKitCoordinator setDrawingTool:_view.currentPage brushSettings:newViewProps.brushSettings];
    }
    if (oldViewProps.iosToolPickerVisible != newViewProps.iosToolPickerVisible) {
        [_view setInMarkupMode:newViewProps.iosToolPickerVisible];
        [_pencilKitCoordinator setToolPickerVisible:_view.currentPage isVisible:newViewProps.iosToolPickerVisible];
    }
    if (oldViewProps.annotationFile != newViewProps.annotationFile) {
        NSString * filePath = [[NSString alloc] initWithUTF8String: newViewProps.annotationFile.c_str()];
        [(MyPDFDocument* )_view.document loadDrawingsFromDisk:filePath];
    }

    [super updateProps:props oldProps:oldProps];
}

- (void)handleCommand:(const NSString *)commandName args:(const NSArray *)args {
    if ([commandName isEqual:@"saveAnnotations"]) {
        if (args.count == 0) {
            return NSLog(@"Missing parameter for loading annotations!");
        }
        [_pencilKitCoordinator prepareForPersistance:(MyPDFDocument *)_view.document];
        [(MyPDFDocument* )_view.document saveDrawingsToDisk:(NSString*) args[0]];
    }
    if ([commandName isEqual:@"loadAnnotations"]) {
        if (args.count == 0) {
            return NSLog(@"Missing parameter for loading annotations!");
        }
        [(MyPDFDocument* )_view.document loadDrawingsFromDisk:(NSString*) args[0]];
    }
}

Class<RCTComponentViewProtocol> PdfAnnotationViewCls(void)
{
    return PdfAnnotationView.class;
}

- hexStringToColor:(NSString *)stringToConvert
{
    NSString *noHashString = [stringToConvert stringByReplacingOccurrencesOfString:@"#" withString:@""];
    NSScanner *stringScanner = [NSScanner scannerWithString:noHashString];

    unsigned hex;
    if (![stringScanner scanHexInt:&hex]) return nil;
    int r = (hex >> 16) & 0xFF;
    int g = (hex >> 8) & 0xFF;
    int b = (hex) & 0xFF;

    return [UIColor colorWithRed:r / 255.0f green:g / 255.0f blue:b / 255.0f alpha:1.0f];
}

@end
