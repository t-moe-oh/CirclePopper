#import <UIKit/UIKit.h>
@import shared;

// iPadOS 26+ types (define manually for SDK compatibility)
#if __IPHONE_OS_VERSION_MAX_ALLOWED < 260000
typedef NS_ENUM(NSInteger, UIWindowSceneWindowingControlStyle) {
    UIWindowSceneWindowingControlStyleAutomatic = 0,
    UIWindowSceneWindowingControlStyleMinimal = 1,
    UIWindowSceneWindowingControlStyleUnified = 2
};
#endif

@interface SceneDelegate : UIResponder <UIWindowSceneDelegate>
@property (strong, nonatomic) UIWindow *window;
@end

@implementation SceneDelegate

- (void)scene:(UIScene *)scene willConnectToSession:(UISceneSession *)session
       options:(UISceneConnectionOptions *)connectionOptions {
    UIWindowScene *windowScene = (UIWindowScene *)scene;
    self.window = [[UIWindow alloc] initWithFrame:windowScene.coordinateSpace.bounds];
    self.window.rootViewController = [SharedMainViewControllerKt MainViewController];
    [self.window makeKeyAndVisible];
}

- (UIWindowSceneWindowingControlStyle)preferredWindowingControlStyleForWindowScene:
    (UIWindowScene *)windowScene {
    if (@available(iOS 26.0, *)) {
        return UIWindowSceneWindowingControlStyleMinimal;
    }
    return UIWindowSceneWindowingControlStyleAutomatic;
}

@end
