import NativePdfAnnotationView, {
    Commands,
    type NativeProps,
} from './PdfAnnotationViewNativeComponent';
import { forwardRef, useImperativeHandle, useRef } from 'react';
import { Platform } from 'react-native';
export * from './PdfAnnotationViewNativeComponent';

type ComponentRef = InstanceType<typeof NativePdfAnnotationView>;

export interface Handle {
    saveAnnotations: (file: string) => void;
    loadAnnotations: (file: string) => void;
}

export const PdfAnnotationView = forwardRef<Handle, NativeProps>(
    ({ brushSettings, ...props }, ref) => {
        const nativeRef = useRef<ComponentRef>(null);

        useImperativeHandle(ref, () => ({
            saveAnnotations(file) {
                if (!nativeRef.current) {
                    return;
                }
                Commands.saveAnnotations(nativeRef.current, file);
            },
            loadAnnotations(file) {
                if (!nativeRef.current) {
                    return;
                }
                Commands.loadAnnotations(nativeRef.current, file);
            },
        }));

        return (
            <NativePdfAnnotationView
                {...props}
                ref={nativeRef}
                brushSettings={Platform.select({
                    ios: brushSettings ?? {
                        type: 'none',
                        color: '#000000',
                        size: 0,
                    },
                    default: brushSettings,
                })}
            />
        );
    }
);
