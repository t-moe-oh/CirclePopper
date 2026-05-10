#import <UIKit/UIKit.h>
@import shared;

@interface AppDelegate : UIResponder <UIApplicationDelegate>
@property (strong, nonatomic) UIWindow *window;
@end

@implementation AppDelegate
- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    self.window = [[UIWindow alloc] initWithFrame:[UIScreen mainScreen].bounds];
    @try {
        self.window.rootViewController = [SharedMainViewControllerKt MainViewController];
    } @catch (NSException *exception) {
        UIViewController *vc = [[UIViewController alloc] init];
        vc.view.backgroundColor = [UIColor blackColor];
        UILabel *label = [[UILabel alloc] initWithFrame:CGRectMake(20, 100, 340, 500)];
        label.text = [NSString stringWithFormat:@"%@\n\n%@", exception.name, exception.reason];
        label.textColor = [UIColor redColor];
        label.backgroundColor = [UIColor clearColor];
        label.numberOfLines = 0;
        label.font = [UIFont systemFontOfSize:14];
        [vc.view addSubview:label];
        self.window.rootViewController = vc;
    }
    [self.window makeKeyAndVisible];
    return YES;
}
@end

int main(int argc, char * argv[]) {
    @autoreleasepool {
        return UIApplicationMain(argc, argv, nil, NSStringFromClass([AppDelegate class]));
    }
}
