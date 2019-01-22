//
//  AgoraMainViewController.m
//  Agora-Online-KTV
//
//  Created by zhanxiaochao on 2018/8/29.
//  Copyright © 2018年 湛孝超. All rights reserved.
//

#import "AgoraMainViewController.h"
#import "AgoraVideoViewController.h"
@interface AgoraMainViewController ()<UITextFieldDelegate>
@property (weak, nonatomic) IBOutlet UIView *popoverSourceView;
@property (weak, nonatomic) IBOutlet UITextField *roomNameTextField;
@property (assign, nonatomic) AgoraVideoProfile videoProfile;
@end

@implementation AgoraMainViewController

- (void)viewDidLoad {
    [super viewDidLoad];


}
- (void)showRoleSelection {
    UIAlertController *sheet = [UIAlertController alertControllerWithTitle:nil message:nil preferredStyle:UIAlertControllerStyleActionSheet];
    UIAlertAction *broadcaster = [UIAlertAction actionWithTitle:@"Broadcaster" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        [self joinWithRole:AgoraClientRoleBroadcaster];
    }];
    UIAlertAction *audience = [UIAlertAction actionWithTitle:@"Audience" style:UIAlertActionStyleDefault handler:^(UIAlertAction * _Nonnull action) {
        [self joinWithRole:AgoraClientRoleAudience];
    }];
    UIAlertAction *cancel = [UIAlertAction actionWithTitle:@"Cancel" style:UIAlertActionStyleDefault handler:nil];
    [sheet addAction:broadcaster];
    [sheet addAction:audience];
    [sheet addAction:cancel];
    [sheet popoverPresentationController].sourceView = self.popoverSourceView;
    [sheet popoverPresentationController].permittedArrowDirections = UIPopoverArrowDirectionUp;
    [self presentViewController:sheet animated:YES completion:nil];
}
- (void)joinWithRole:(AgoraClientRole)role {
    [self performSegueWithIdentifier:@"mainToLive" sender:@(role)];
}
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    NSString *segueId = segue.identifier;
    
 if ([segueId isEqualToString:@"mainToLive"]) {
        AgoraVideoViewController *liveVC = segue.destinationViewController;
        liveVC.roomName = self.roomNameTextField.text;
        liveVC.videoProfile = self.videoProfile;
        liveVC.clientRole = [sender integerValue];
    }
}

- (BOOL)textFieldShouldReturn:(UITextField *)textField {
    
    if (textField.text.length) {
        [self showRoleSelection];
    }
    
    return YES;
}
- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
