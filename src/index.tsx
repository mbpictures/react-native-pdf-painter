import NativePdfAnnotationView, {
    Commands,
    type NativeProps,
    type PageChangeEvent,
    type PageCountEvent,
} from './PdfAnnotationViewNativeComponent';
import {
    forwardRef,
    type ReactElement,
    useEffect,
    useImperativeHandle,
    useRef,
    useState,
} from 'react';
import {
    Platform,
    type StyleProp,
    StyleSheet,
    TouchableOpacity,
    View,
    type ViewStyle,
} from 'react-native';
import type { BubblingEventHandler } from 'react-native/Libraries/Types/CodegenTypes';
export * from './PdfAnnotationViewNativeComponent';

type ComponentRef = InstanceType<typeof NativePdfAnnotationView>;

export interface Handle {
    saveAnnotations: (file: string) => void;
    loadAnnotations: (file: string) => void;
    undo: () => void;
    redo: () => void;
    clear: () => void;
}

export interface Props
    extends Omit<NativeProps, 'onPageCount' | 'onPageChange'> {
    onPageChange?: (currentPage: number) => unknown;
    onPageCount?: (pageCount: number) => unknown;
    renderPageIndicatorItem?: (props: PageIndicatorProps) => ReactElement;
    currentPage?: number;
    containerStyles?: StyleProp<ViewStyle>;
}

export interface PageIndicatorProps {
    onClick: () => unknown;
    active: boolean;
}

export const PageIndicator = ({ onClick, active }: PageIndicatorProps) => {
    return (
        <TouchableOpacity onPress={onClick}>
            <View
                style={[
                    styles.pageIndicator,
                    active && styles.pageIndicatorActive,
                ]}
            />
        </TouchableOpacity>
    );
};

export const PdfAnnotationView = forwardRef<Handle, Props>(
    (
        {
            brushSettings,
            currentPage,
            style,
            renderPageIndicatorItem,
            onPageCount,
            onPageChange,
            containerStyles,
            ...props
        },
        ref
    ) => {
        const nativeRef = useRef<ComponentRef>(null);
        const [stateCurrentPage, setStateCurrentPage] = useState(0);
        const [pageCount, setPageCount] = useState(0);

        useEffect(() => {
            if (!nativeRef.current) {
                return;
            }
            Commands.setPage(
                nativeRef.current,
                currentPage ?? stateCurrentPage
            );
        }, [stateCurrentPage, currentPage]);

        const handlePageCount: BubblingEventHandler<PageCountEvent> = (
            event
        ) => {
            setPageCount(event.nativeEvent.pageCount);
            onPageCount?.(event.nativeEvent.pageCount);
        };

        const handlePageChange: BubblingEventHandler<PageChangeEvent> = (
            event
        ) => {
            setStateCurrentPage(event.nativeEvent.currentPage);
            onPageChange?.(event.nativeEvent.currentPage);
        };

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
            undo() {
                if (!nativeRef.current) {
                    return;
                }
                Commands.undo(nativeRef.current);
            },
            redo() {
                if (!nativeRef.current) {
                    return;
                }
                Commands.redo(nativeRef.current);
            },
            clear() {
                if (!nativeRef.current) {
                    return;
                }
                Commands.clear(nativeRef.current);
            },
            setPage(page: number) {
                if (!nativeRef.current) {
                    return;
                }
                Commands.setPage(nativeRef.current, page);
            },
        }));

        const RenderItem = renderPageIndicatorItem ?? PageIndicator;

        return (
            <View style={[styles.container, containerStyles]}>
                <NativePdfAnnotationView
                    {...props}
                    style={[styles.viewer, style]}
                    ref={nativeRef}
                    brushSettings={Platform.select({
                        ios: brushSettings ?? {
                            type: 'none',
                            color: '#000000',
                            size: 0,
                        },
                        default: brushSettings,
                    })}
                    onPageCount={handlePageCount}
                    onPageChange={handlePageChange}
                />
                {!props.thumbnailMode && (
                    <View style={styles.pageIndicatorContainer}>
                        {Array.from(Array(pageCount).keys()).map((i) => (
                            <RenderItem
                                key={i}
                                active={(currentPage ?? stateCurrentPage) === i}
                                onClick={() => {
                                    if (currentPage && onPageChange) {
                                        onPageChange(i);
                                        return;
                                    }
                                    setStateCurrentPage(i);
                                }}
                            />
                        ))}
                    </View>
                )}
            </View>
        );
    }
);

const styles = StyleSheet.create({
    container: {
        flex: 1,
    },
    pageIndicatorContainer: {
        position: 'absolute',
        bottom: 5,
        width: '100%',
        display: 'flex',
        flexDirection: 'row',
        justifyContent: 'center',
    },
    pageIndicator: {
        width: 20,
        height: 20,
        borderRadius: 10,
        backgroundColor: 'grey',
        marginHorizontal: 5,
    },
    pageIndicatorActive: {
        backgroundColor: 'darkgrey',
    },
    viewer: {
        flex: 1,
    },
});
