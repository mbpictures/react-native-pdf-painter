import {
    View,
    StyleSheet,
    Alert,
    Button,
    TouchableHighlight,
    Text,
    Platform,
} from 'react-native';
import {
    type BrushSettings,
    type Handle,
    PdfAnnotationView,
} from 'react-native-pdf-painter';
import { type ReactNode, useRef, useState } from 'react';
import { types, pick } from '@react-native-documents/picker';
import {
    CheckIcon,
    HighlighterIcon,
    EraserIcon,
    PencilIcon,
    PenIcon,
    XIcon,
    PaletteIcon,
    UndoIcon,
    RedoIcon,
    LinkIcon,
    ChevronRight,
    ChevronLeft,
} from 'lucide-react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

const BRUSH_SETTINGS: { settings: BrushSettings; icon: ReactNode }[] = [
    {
        settings: {
            type: 'marker',
            size: 3,
            color: '#000000',
        },
        icon: <PenIcon />,
    },
    {
        settings: {
            type: 'pressure-pen',
            size: 3,
            color: '#000000',
        },
        icon: <PencilIcon />,
    },
    {
        settings: {
            type: 'highlighter',
            size: 15,
            color: '#88FFFF00',
        },
        icon: <HighlighterIcon />,
    },
    {
        settings: {
            type: 'eraser',
            size: 5,
            color: '#000000',
        },
        icon: <EraserIcon />,
    },
    {
        settings: {
            type: 'link',
            size: 75,
            color: '#7700FF00',
        },
        icon: <LinkIcon />,
    },
];

const compareSettings = (a?: BrushSettings, b?: BrushSettings) => {
    return a?.type === b?.type && a?.size === b?.size && a?.color === b?.color;
};

const getAnnotationsPath = (file: string | null) => {
    if (!file) return null;
    const filename = file
        .substring(file.lastIndexOf('/') + 1, file.length)
        .replace('.pdf', '.ant');
    const folder = file.substring(
        0,
        file.substring(0, file.lastIndexOf('/')).lastIndexOf('/')
    );
    return folder + '/' + filename;
};

export default function App() {
    const [pdfFile, setPdfFile] = useState<string | null>(null);
    const [brush, setBrush] = useState<BrushSettings | undefined>(undefined);
    const [thumbnail, setThumbnail] = useState(false);
    const [iosToolbar, setIosToolbar] = useState(false);
    const pdfViewer = useRef<Handle>(null);
    const annotationFile = getAnnotationsPath(pdfFile);
    const currentPage = useRef(0);

    const handleSelectFile = async () => {
        try {
            const response = await pick({
                type: types.pdf,
                copyTo: 'documentDirectory',
            });
            setPdfFile(response[0].uri);
        } catch (e) {
            Alert.alert('File Selection Error');
        }
    };

    const handleClickTool = (settings: BrushSettings) => () => {
        setBrush((oldBrush) => {
            if (compareSettings(oldBrush, settings)) {
                return undefined;
            }

            return settings;
        });
    };

    const handleSaveAnnotations = () => {
        if (!annotationFile) return;
        pdfViewer.current?.saveAnnotations(annotationFile);
    };

    const handleUndo = () => pdfViewer.current?.undo();

    const handleRedo = () => pdfViewer.current?.redo();

    const handleClear = () => pdfViewer.current?.clear();

    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.topBar}>
                <Button title={'Select PDF'} onPress={handleSelectFile} />
                {pdfFile && (
                    <>
                        <Button
                            title={'Save Annotations'}
                            onPress={handleSaveAnnotations}
                        />
                        <TouchableHighlight
                            onPress={() => setThumbnail((o) => !o)}
                        >
                            <View style={styles.thumbnailButton}>
                                <Text>Thumbnail: </Text>
                                {thumbnail ? <CheckIcon /> : <XIcon />}
                            </View>
                        </TouchableHighlight>
                    </>
                )}
            </View>
            {pdfFile && (
                <>
                    <PdfAnnotationView
                        pdfUrl={pdfFile}
                        annotationFile={annotationFile ?? undefined}
                        style={styles.box}
                        brushSettings={brush}
                        ref={pdfViewer}
                        thumbnailMode={thumbnail}
                        iosToolPickerVisible={iosToolbar}
                        autoSave
                        onDocumentFinished={(direction) =>
                            console.log(`OnDocumentFinished: ${direction}`)
                        }
                        onTap={(e) => console.log(e.nativeEvent)}
                        onLinkCompleted={() => setBrush(undefined)}
                        onPageChange={(page) => (currentPage.current = page)}
                        backgroundColor="#FFFFFF"
                    />
                    {brush && brush.type !== 'none' && (
                        <View style={styles.pageNavigation}>
                            <TouchableHighlight
                                underlayColor={'#ffe9d2'}
                                style={styles.toolbarItem}
                                onPress={() =>
                                    pdfViewer.current?.setPage(
                                        currentPage.current - 1
                                    )
                                }
                            >
                                <ChevronLeft />
                            </TouchableHighlight>
                            <TouchableHighlight
                                underlayColor={'#ffe9d2'}
                                style={styles.toolbarItem}
                                onPress={() =>
                                    pdfViewer.current?.setPage(
                                        currentPage.current + 1
                                    )
                                }
                            >
                                <ChevronRight />
                            </TouchableHighlight>
                        </View>
                    )}
                    <View style={styles.toolbar}>
                        {BRUSH_SETTINGS.map((config, i) => (
                            <TouchableHighlight
                                key={i}
                                underlayColor={'#ffe9d2'}
                                style={[
                                    styles.toolbarItem,
                                    compareSettings(brush, config.settings) &&
                                        styles.toolbarItemActive,
                                ]}
                                onPress={handleClickTool(config.settings)}
                            >
                                {config.icon}
                            </TouchableHighlight>
                        ))}
                        {Platform.OS === 'ios' && (
                            <TouchableHighlight
                                underlayColor={'#ffe9d2'}
                                style={[
                                    styles.toolbarItem,
                                    iosToolbar && styles.toolbarItemActive,
                                ]}
                                onPress={() => setIosToolbar((o) => !o)}
                            >
                                <PaletteIcon />
                            </TouchableHighlight>
                        )}
                        <TouchableHighlight
                            underlayColor={'#ffe9d2'}
                            style={styles.toolbarItem}
                            onPress={handleUndo}
                        >
                            <UndoIcon />
                        </TouchableHighlight>
                        <TouchableHighlight
                            underlayColor={'#ffe9d2'}
                            style={styles.toolbarItem}
                            onPress={handleRedo}
                        >
                            <RedoIcon />
                        </TouchableHighlight>
                        <TouchableHighlight
                            underlayColor={'#ffe9d2'}
                            style={styles.toolbarItem}
                            onPress={handleClear}
                        >
                            <XIcon />
                        </TouchableHighlight>
                    </View>
                </>
            )}
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: 'white',
    },
    topBar: {
        flexDirection: 'row',
        padding: 6,
        justifyContent: 'space-between',
    },
    box: {
        flex: 1,
    },
    toolbar: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        shadowColor: '#000',
        shadowOffset: {
            width: 0,
            height: 10,
        },
        shadowOpacity: 0.39,
        shadowRadius: 4,
        elevation: 13,
    },
    thumbnailButton: {
        padding: 6,
        flexDirection: 'row',
        gap: 2,
        backgroundColor: 'lightgray',
    },
    toolbarItem: {
        margin: 6,
        borderRadius: 20,
        borderWidth: 1,
        borderColor: 'black',
        padding: 6,
    },
    toolbarItemActive: {
        backgroundColor: '#ffcc9c',
    },
    pageNavigation: {
        position: 'absolute',
        top: '50%',
        left: 0,
        width: '100%',
        transform: [
            {
                translateY: '-50%',
            },
        ],
        flexDirection: 'row',
        justifyContent: 'space-between',
    },
});
